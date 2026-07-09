package com.leathalenterprises.onyx

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.leathalenterprises.onyx.data.GemmaLabeler
import com.leathalenterprises.onyx.data.LabelRequest
import com.leathalenterprises.onyx.data.LabelValidator
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

/**
 * On-device behavioral tests for the real Gemma model: does it actually
 * produce the labels we want for realistic app sets?
 *
 * FIXME(gemma): remove @Ignore once the model is wired in (see GemmaLabeler).
 * These need a device/emulator with the .task bundle pushed to MODEL_PATH:
 *   adb push gemma3-270m-it.task /data/local/tmp/gemma3-270m-it.task
 * If the 270M model fails these, try the 1B bundle before prompt surgery.
 */
@Ignore("FIXME(gemma): requires the real on-device model; see GemmaLabeler")
@RunWith(AndroidJUnit4::class)
class GemmaModelLabelingTest {

    private val labeler = GemmaLabeler(MODEL_PATH)

    private fun labelAll(requests: List<LabelRequest>): Map<String, String> =
        LabelValidator.finalize(requests, runBlocking { labeler.label(requests) })

    @Test
    fun brandedSingletonsGetGenericLabels() {
        val finals = labelAll(
            listOf(
                LabelRequest(
                    "com.google.android.apps.messaging", "Google Messages",
                    category = null, role = "default messaging app",
                ),
                LabelRequest("com.android.vending", "Google Play Store", null, null),
                LabelRequest(
                    "com.android.chrome", "Chrome",
                    category = null, role = "default web browser",
                ),
            ),
        )
        assertEquals("Messages", finals["com.google.android.apps.messaging"])
        assertEquals("Store", finals["com.android.vending"])
        assertEquals("Browser", finals["com.android.chrome"])
    }

    @Test
    fun sameCategoryAppsKeepTheirBrandNames() {
        // Three ride-share apps: a generic label would be ambiguous, so the
        // model (or the validator behind it) must keep the brand names.
        val finals = labelAll(
            listOf(
                LabelRequest("com.ubercab", "Uber", "MAPS", null),
                LabelRequest("me.lyft.android", "Lyft", "MAPS", null),
                LabelRequest("com.waymo.carapp", "Waymo One", "MAPS", null),
            ),
        )
        assertEquals("Uber", finals["com.ubercab"])
        assertEquals("Lyft", finals["me.lyft.android"])
        assertEquals("Waymo One", finals["com.waymo.carapp"])
    }

    @Test
    fun mixedSetLabelsTheUnambiguousAndKeepsTheAmbiguous() {
        val finals = labelAll(
            listOf(
                LabelRequest("com.ubercab", "Uber", "MAPS", null),
                LabelRequest("me.lyft.android", "Lyft", "MAPS", null),
                LabelRequest("com.spotify.music", "Spotify", "AUDIO", null),
                LabelRequest("org.thoughtcrime.securesms", "Signal", "SOCIAL", null),
            ),
        )
        assertEquals("Uber", finals["com.ubercab"])
        assertEquals("Lyft", finals["me.lyft.android"])
        assertEquals("Music", finals["com.spotify.music"])
        assertEquals("Messages", finals["org.thoughtcrime.securesms"])
    }

    @Test
    fun unknownAppKeepsItsOriginalName() {
        val finals = labelAll(
            listOf(
                LabelRequest("com.example.zzgarblenator", "Garblenator", null, null),
            ),
        )
        assertEquals("Garblenator", finals["com.example.zzgarblenator"])
    }

    private companion object {
        const val MODEL_PATH = "/data/local/tmp/gemma3-270m-it.task"
    }
}
