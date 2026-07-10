package com.leathalenterprises.onyx.data

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * On-device LLM labeler backed by MediaPipe LLM Inference running Gemma 3
 * (1B-it int4; 270M could not follow instructions reliably).
 *
 * Asks one tiny question per app instead of one batch-JSON question for the
 * set: small models answer "one word for what this app does" far more
 * reliably than they emit structured mappings (observed failure modes of the
 * batch approach: parroted few-shot examples, invented schemas, two-word
 * labels, package fragments echoed into keys). Collisions between generic
 * answers are handled deterministically by [LabelValidator], which reverts
 * same-label apps to their brand names.
 *
 * The model file is not shipped in the APK. Get it onto the device with:
 *   adb push gemma3-1b-it-int4.task /data/local/tmp/gemma3.task
 *   adb shell run-as com.leathalenterprises.onyx sh -c \
 *     'cp /data/local/tmp/gemma3.task files/'
 * (or download it into context.filesDir at first run, someday.)
 */
class GemmaLabeler(
    context: Context,
    private val modelPath: String,
) : Labeler {

    private val context = context.applicationContext

    override suspend fun label(requests: List<LabelRequest>): Map<String, String>? {
        if (!File(modelPath).exists()) return null
        // Apps holding a system role have a known function; label those
        // deterministically and only ask the model about the rest.
        val roleLabels = requests.mapNotNull { request ->
            ROLE_LABELS[request.role]?.let { request.packageName to it }
        }.toMap()
        val unresolved = requests.filter { it.packageName !in roleLabels }
        if (unresolved.isEmpty()) return roleLabels

        return withContext(Dispatchers.Default) {
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .setMaxTokens(512)
                .build()
            LlmInference.createFromOptions(context, options).use { llm ->
                roleLabels + unresolved.associate { request ->
                    request.packageName to labelOne(llm, request)
                }
            }
        }
    }

    private fun labelOne(llm: LlmInference, request: LabelRequest): String {
        val sessionOptions = LlmInferenceSession.LlmInferenceSessionOptions.builder()
            // Deterministic decoding: labels should never be creative, and
            // greedy decoding makes same-function apps converge on the same
            // word, which is what lets the validator catch collisions.
            .setTemperature(0f)
            .setTopK(1)
            .build()
        return LlmInferenceSession.createFromOptions(llm, sessionOptions).use { session ->
            session.addQueryChunk(buildPrompt(request))
            val raw = session.generateResponse()
            Log.d(TAG, "${request.packageName} -> $raw")
            val label = extractLabel(raw)
            // "I don't know" answers become empty proposals, which the
            // validator resolves to the app's real name.
            if (label.equals(UNKNOWN, ignoreCase = true)) "" else label
        }
    }

    companion object {

        private const val TAG = "GemmaLabeler"

        /** Sentinel the model answers when it doesn't know the app. */
        const val UNKNOWN = "Unknown"

        /** Deterministic labels for apps holding a system role. */
        val ROLE_LABELS: Map<String, String> = mapOf(
            "default messaging app" to "Messages",
            "default phone app" to "Phone",
            "default web browser" to "Browser",
        )

        /** One question about one app, answerable with a single word. */
        fun buildPrompt(request: LabelRequest): String = buildString {
            append("What is the Android app \"")
            append(request.originalLabel)
            append("\" (package ")
            append(request.packageName)
            append(')')
            request.role?.let { append("; it is the ").append(it) }
            request.category?.let { append("; its category is ").append(it) }
            appendLine(" used for?")
            appendLine(
                "Good answers look like: Store, Messages, Browser, Music, " +
                    "Mail, Photos, Maps, Camera, Ride, Notes, Weather, " +
                    "Bank, or Fitness."
            )
            appendLine("If you are not sure what the app does, answer: $UNKNOWN")
            append("Answer with one word only.")
        }

        /**
         * Cleans a model reply down to its answer: strips code fences,
         * quotes, and trailing punctuation, and keeps the first line.
         * Multi-word answers are returned as-is — the validator rejects
         * them, falling back to the app's real name.
         */
        fun extractLabel(raw: String): String =
            raw.lineSequence()
                .map { it.trim().trim('`', '"', '\'', '*', ' ') }
                .firstOrNull { it.isNotEmpty() && it != "json" }
                ?.trimEnd('.', '!', ',')
                ?: ""
    }
}
