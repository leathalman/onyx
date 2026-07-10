package com.leathalenterprises.onyx

import com.leathalenterprises.onyx.data.LabelRequest
import com.leathalenterprises.onyx.data.LabelValidator
import org.junit.Assert.assertEquals
import org.junit.Test

class LabelValidatorTest {

    private fun req(pkg: String, name: String) =
        LabelRequest(packageName = pkg, originalLabel = name, category = null, role = null)

    @Test
    fun `valid proposals are used`() {
        val requests = listOf(req("com.android.vending", "Google Play Store"))
        val finals = LabelValidator.finalize(
            requests,
            mapOf("com.android.vending" to "Store"),
        )
        assertEquals("Store", finals["com.android.vending"])
    }

    @Test
    fun `null proposals fall back to original names`() {
        val requests = listOf(req("com.ubercab", "Uber"))
        val finals = LabelValidator.finalize(requests, null)
        assertEquals("Uber", finals["com.ubercab"])
    }

    @Test
    fun `missing proposal falls back to original name`() {
        val requests = listOf(
            req("com.android.vending", "Google Play Store"),
            req("com.ubercab", "Uber"),
        )
        val finals = LabelValidator.finalize(
            requests,
            mapOf("com.android.vending" to "Store"),
        )
        assertEquals("Store", finals["com.android.vending"])
        assertEquals("Uber", finals["com.ubercab"])
    }

    @Test
    fun `multi word proposals are rejected`() {
        val requests = listOf(req("com.android.vending", "Google Play Store"))
        val finals = LabelValidator.finalize(
            requests,
            mapOf("com.android.vending" to "App Store"),
        )
        assertEquals("Google Play Store", finals["com.android.vending"])
    }

    @Test
    fun `overlong proposals are rejected`() {
        val requests = listOf(req("com.ubercab", "Uber"))
        val finals = LabelValidator.finalize(
            requests,
            mapOf("com.ubercab" to "Transportation"),
        )
        assertEquals("Uber", finals["com.ubercab"])
    }

    @Test
    fun `empty proposals are rejected`() {
        val requests = listOf(req("com.ubercab", "Uber"))
        val finals = LabelValidator.finalize(requests, mapOf("com.ubercab" to " "))
        assertEquals("Uber", finals["com.ubercab"])
    }

    @Test
    fun `colliding generic labels revert to brand names`() {
        // The Uber/Lyft case: a labeler that says "Rides" for both must not
        // produce two identical home-screen entries.
        val requests = listOf(
            req("com.ubercab", "Uber"),
            req("me.lyft.android", "Lyft"),
        )
        val finals = LabelValidator.finalize(
            requests,
            mapOf("com.ubercab" to "Rides", "me.lyft.android" to "Rides"),
        )
        assertEquals("Uber", finals["com.ubercab"])
        assertEquals("Lyft", finals["me.lyft.android"])
    }

    @Test
    fun `singular and plural labels count as a collision`() {
        val requests = listOf(
            req("com.ubercab", "Uber"),
            req("com.waymo.carapp", "Waymo One"),
        )
        val finals = LabelValidator.finalize(
            requests,
            mapOf("com.ubercab" to "Map", "com.waymo.carapp" to "Maps"),
        )
        assertEquals("Uber", finals["com.ubercab"])
        assertEquals("Waymo One", finals["com.waymo.carapp"])
    }

    @Test
    fun `collision detection is case insensitive`() {
        val requests = listOf(
            req("com.ubercab", "Uber"),
            req("me.lyft.android", "Lyft"),
        )
        val finals = LabelValidator.finalize(
            requests,
            mapOf("com.ubercab" to "Rides", "me.lyft.android" to "rides"),
        )
        assertEquals("Uber", finals["com.ubercab"])
        assertEquals("Lyft", finals["me.lyft.android"])
    }

    @Test
    fun `non colliding proposals survive alongside a collision`() {
        val requests = listOf(
            req("com.ubercab", "Uber"),
            req("me.lyft.android", "Lyft"),
            req("com.android.vending", "Google Play Store"),
        )
        val finals = LabelValidator.finalize(
            requests,
            mapOf(
                "com.ubercab" to "Rides",
                "me.lyft.android" to "Rides",
                "com.android.vending" to "Store",
            ),
        )
        assertEquals("Uber", finals["com.ubercab"])
        assertEquals("Lyft", finals["me.lyft.android"])
        assertEquals("Store", finals["com.android.vending"])
    }
}
