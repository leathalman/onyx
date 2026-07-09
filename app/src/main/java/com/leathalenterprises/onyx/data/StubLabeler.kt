package com.leathalenterprises.onyx.data

import kotlinx.coroutines.delay

/**
 * FIXME(gemma): delete once GemmaLabeler runs a real model. This stands in
 * for it today: fakes inference latency (so the fuzz animation is feelable
 * on the emulator) and labels by keyword lookup instead of a model.
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
