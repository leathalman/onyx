package com.leathalenterprises.onyx

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.leathalenterprises.onyx.data.GemmaLabeler
import com.leathalenterprises.onyx.data.LabelRequest
import com.leathalenterprises.onyx.data.LabelValidator
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * On-device behavioral tests for the real Gemma model: does it actually
 * produce the labels we want for realistic app sets?
 *
 * Needs the model bundle in the app's files dir (tests skip when absent):
 *   adb push <model>.task /data/local/tmp/gemma3.task
 *   adb shell run-as com.leathalenterprises.onyx sh -c \
 *     'cp /data/local/tmp/gemma3.task files/'
 * Verdicts so far: 270M can't hold the batch-JSON task (parrots few-shot
 * examples or invents its own schema); 1B is the working floor.
 */
@RunWith(AndroidJUnit4::class)
class GemmaModelLabelingTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val modelFile = File(context.filesDir, "gemma3.task")
    private val labeler = GemmaLabeler(context, modelFile.path)

    @Before
    fun requireModel() {
        assumeTrue("model bundle not on device; skipping", modelFile.exists())
    }

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
        // Synonyms are fine; the point is the brand is gone.
        assertTrue(
            "got ${finals["com.google.android.apps.messaging"]}",
            finals["com.google.android.apps.messaging"] in setOf("Messages", "Messaging"),
        )
        assertTrue(
            "got ${finals["com.android.vending"]}",
            finals["com.android.vending"] in setOf("Store", "Shop", "Apps"),
        )
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
        assertTrue(
            "got ${finals["org.thoughtcrime.securesms"]}",
            finals["org.thoughtcrime.securesms"] in setOf("Messages", "Messaging"),
        )
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
}
