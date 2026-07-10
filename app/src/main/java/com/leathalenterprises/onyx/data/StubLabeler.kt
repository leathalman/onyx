package com.leathalenterprises.onyx.data

import kotlinx.coroutines.delay

/**
 * Fallback labeler for devices without the Gemma model file (emulators,
 * fresh installs): keyword lookup instead of a model, plus fake latency so
 * the fuzz animation still reads correctly.
 */
class StubLabeler : Labeler {

    override suspend fun label(requests: List<LabelRequest>): Map<String, String>? {
        delay(FAKE_INFERENCE_MS)
        return requests.associate { request ->
            val name = request.originalLabel.lowercase()
            val generic = KEYWORDS.firstOrNull { (words, _) ->
                words.any { it in name }
            }?.second
            request.packageName to (generic ?: request.originalLabel)
        }
    }

    private companion object {
        const val FAKE_INFERENCE_MS = 2_000L

        val KEYWORDS: List<Pair<List<String>, String>> = listOf(
            listOf("play store") to "Store",
            listOf("message", "sms") to "Messages",
            listOf("chrome", "firefox", "browser") to "Browser",
            listOf("camera") to "Camera",
            listOf("photo", "gallery") to "Photos",
            listOf("gmail", "mail") to "Mail",
            listOf("maps") to "Maps",
            listOf("music", "spotify") to "Music",
            listOf("clock") to "Clock",
            listOf("calendar") to "Calendar",
            listOf("phone", "dialer") to "Phone",
        )
    }
}
