package com.leathalenterprises.onyx.data

/**
 * On-device LLM labeler, to be backed by MediaPipe LLM Inference running a
 * Gemma 3 model (270M-it int4 to start; bump to 1B if labels are weak).
 *
 * FIXME(gemma): inert until the real model is wired in. On real wifi:
 *  1. app/build.gradle.kts: implementation("com.google.mediapipe:tasks-genai:<latest>")
 *  2. Download the Gemma 3 270M-it int4 LiteRT `.task` bundle (Hugging Face,
 *     litert-community) into context.filesDir — or `adb push` to
 *     /data/local/tmp for development.
 *  3. Implement label() per the sketch below.
 *  4. Swap StubLabeler for this class in MainActivity.
 *  5. Un-@Ignore GemmaModelLabelingTest in androidTest and run it on-device.
 */
class GemmaLabeler(
    @Suppress("unused") private val modelPath: String,
) : Labeler {

    override suspend fun label(requests: List<LabelRequest>): Map<String, String>? {
        // FIXME(gemma): the real implementation, roughly:
        //
        // val options = LlmInference.LlmInferenceOptions.builder()
        //     .setModelPath(modelPath)
        //     .setMaxTokens(256)
        //     .build()
        // val llm = LlmInference.createFromOptions(context, options)
        // val raw = withContext(Dispatchers.Default) {
        //     llm.generateResponse(buildPrompt(requests))
        // }
        // llm.close()
        // return parseResponse(raw)
        //
        // Keep temperature at/near 0 (deterministic labels), and keep the
        // LlmInference instance alive across the debounce window if warm
        // starts matter (create when the picker opens, close when it exits).
        return null
    }

    companion object {

        /**
         * One batch prompt for the whole selected set, so the model can solve
         * the collision problem (Uber + Lyft + Waymo) in one pass.
         */
        fun buildPrompt(requests: List<LabelRequest>): String = buildString {
            appendLine(
                "You assign short generic labels to smartphone apps for a " +
                    "minimal text-only launcher."
            )
            appendLine("Rules:")
            appendLine("- One word per label, at most 12 characters.")
            appendLine(
                "- Prefer a generic noun for what the app does, like " +
                    "\"Store\", \"Messages\", or \"Browser\"."
            )
            appendLine(
                "- Labels must be unique. If several apps do the same " +
                    "thing, keep their brand names instead."
            )
            appendLine("- If you do not recognize an app, return its name unchanged.")
            appendLine("Apps:")
            requests.forEach { request ->
                append("- package=").append(request.packageName)
                append(" name=\"").append(request.originalLabel).append('"')
                request.category?.let { append(" category=").append(it) }
                request.role?.let { append(" role=\"").append(it).append('"') }
                appendLine()
            }
            append(
                "Answer with only a JSON object mapping each package name " +
                    "to its label, and nothing else."
            )
        }

        /**
         * Extracts a package-to-label map from raw model output. Tolerates
         * prose or code fences around the JSON; returns null when nothing
         * usable is present. Regex-based on purpose: keeps this parsable in
         * plain JVM unit tests (org.json is stubbed out there) and immune to
         * a small model's imperfect JSON.
         */
        fun parseResponse(raw: String): Map<String, String>? {
            val body = raw.substringAfter('{', "").substringBeforeLast('}', "")
            if (body.isEmpty()) return null
            return Regex("\"([^\"]+)\"\\s*:\\s*\"([^\"]*)\"")
                .findAll(body)
                .associate { it.groupValues[1] to it.groupValues[2] }
                .ifEmpty { null }
        }
    }
}
