package com.leathalenterprises.onyx

import com.leathalenterprises.onyx.data.GemmaLabeler
import com.leathalenterprises.onyx.data.LabelRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** JVM tests for the prompt/response plumbing around the model. */
class GemmaLabelerTest {

    @Test
    fun `prompt lists every app with package and name`() {
        val prompt = GemmaLabeler.buildPrompt(
            listOf(
                LabelRequest("com.ubercab", "Uber", null, null),
                LabelRequest("com.android.vending", "Google Play Store", null, null),
            ),
        )
        assertTrue(prompt.contains("package=com.ubercab"))
        assertTrue(prompt.contains("name=\"Uber\""))
        assertTrue(prompt.contains("package=com.android.vending"))
        assertTrue(prompt.contains("name=\"Google Play Store\""))
    }

    @Test
    fun `prompt includes category and role hints when known`() {
        val prompt = GemmaLabeler.buildPrompt(
            listOf(
                LabelRequest(
                    packageName = "com.android.chrome",
                    originalLabel = "Chrome",
                    category = "PRODUCTIVITY",
                    role = "default web browser",
                ),
            ),
        )
        assertTrue(prompt.contains("category=PRODUCTIVITY"))
        assertTrue(prompt.contains("role=\"default web browser\""))
    }

    @Test
    fun `prompt states the uniqueness and brand-name rules`() {
        val prompt = GemmaLabeler.buildPrompt(
            listOf(LabelRequest("com.ubercab", "Uber", null, null)),
        )
        assertTrue(prompt.contains("unique", ignoreCase = true))
        assertTrue(prompt.contains("brand", ignoreCase = true))
        assertTrue(prompt.contains("unchanged", ignoreCase = true))
    }

    @Test
    fun `parses a plain json response`() {
        val parsed = GemmaLabeler.parseResponse(
            """{"com.ubercab": "Uber", "com.android.vending": "Store"}""",
        )
        assertEquals(
            mapOf("com.ubercab" to "Uber", "com.android.vending" to "Store"),
            parsed,
        )
    }

    @Test
    fun `parses json wrapped in prose and code fences`() {
        val parsed = GemmaLabeler.parseResponse(
            """
            Sure! Here are the labels:
            ```json
            {"com.android.vending": "Store"}
            ```
            """.trimIndent(),
        )
        assertEquals(mapOf("com.android.vending" to "Store"), parsed)
    }

    @Test
    fun `garbage output returns null`() {
        assertNull(GemmaLabeler.parseResponse("I cannot label these apps."))
        assertNull(GemmaLabeler.parseResponse(""))
        assertNull(GemmaLabeler.parseResponse("{}"))
    }
}
