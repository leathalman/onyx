package com.leathalenterprises.onyx

import com.leathalenterprises.onyx.data.GemmaLabeler
import com.leathalenterprises.onyx.data.LabelRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** JVM tests for the prompt/response plumbing around the model. */
class GemmaLabelerTest {

    @Test
    fun `prompt names the app and its package`() {
        val prompt = GemmaLabeler.buildPrompt(
            LabelRequest("com.android.vending", "Google Play Store", null, null),
        )
        assertTrue(prompt.contains("\"Google Play Store\""))
        assertTrue(prompt.contains("com.android.vending"))
    }

    @Test
    fun `prompt includes the role when known`() {
        val prompt = GemmaLabeler.buildPrompt(
            LabelRequest("com.android.chrome", "Chrome", null, "default web browser"),
        )
        assertTrue(prompt.contains("it is the default web browser"))
    }

    @Test
    fun `prompt demands one word and offers the unknown sentinel`() {
        val prompt = GemmaLabeler.buildPrompt(
            LabelRequest("com.example.foo", "Foo", null, null),
        )
        assertTrue(prompt.contains("one word only", ignoreCase = true))
        assertTrue(prompt.contains("answer: Unknown"))
    }

    @Test
    fun `prompt includes the category when known`() {
        val prompt = GemmaLabeler.buildPrompt(
            LabelRequest("com.ubercab", "Uber", "MAPS", null),
        )
        assertTrue(prompt.contains("its category is MAPS"))
    }

    @Test
    fun `system roles map to deterministic labels`() {
        assertEquals("Messages", GemmaLabeler.ROLE_LABELS["default messaging app"])
        assertEquals("Phone", GemmaLabeler.ROLE_LABELS["default phone app"])
        assertEquals("Browser", GemmaLabeler.ROLE_LABELS["default web browser"])
    }

    @Test
    fun `plain answers pass through`() {
        assertEquals("Store", GemmaLabeler.extractLabel("Store"))
    }

    @Test
    fun `fences quotes and punctuation are stripped`() {
        assertEquals("Store", GemmaLabeler.extractLabel("```\nStore\n```"))
        assertEquals("Store", GemmaLabeler.extractLabel("\"Store\"."))
        assertEquals("Store", GemmaLabeler.extractLabel("  Store!\n"))
    }

    @Test
    fun `first non-empty line wins`() {
        assertEquals("Browser", GemmaLabeler.extractLabel("\n\nBrowser\nBecause it browses."))
    }

    @Test
    fun `multi-word answers are preserved for the validator to reject`() {
        // The validator turns these into the app's real name; extraction
        // must not "help" by picking one of the words.
        assertEquals("Web Browser", GemmaLabeler.extractLabel("Web Browser"))
    }

    @Test
    fun `empty output becomes empty string`() {
        assertEquals("", GemmaLabeler.extractLabel(""))
        assertEquals("", GemmaLabeler.extractLabel("```json\n```"))
    }
}
