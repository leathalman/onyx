package com.leathalenterprises.onyx.data

/**
 * Everything the labeler may know about one selected app. Pure strings so
 * label logic stays JVM-unit-testable (no Android types).
 */
data class LabelRequest(
    val packageName: String,
    val originalLabel: String,
    /** Manifest `android:appCategory` name (e.g. "MAPS"), if declared. */
    val category: String?,
    /** System role, if any (e.g. "default web browser"). */
    val role: String?,
)

/**
 * Proposes generic home-screen labels ("Store", "Messages") for the selected
 * apps. Returns a map of package name to proposed label, or null if no
 * proposals could be made. Proposals are suggestions only — every result goes
 * through [LabelValidator], and anything invalid falls back to the app's
 * real name. Implementations may be slow (on-device LLM); callers must not
 * block UI on this.
 */
interface Labeler {
    suspend fun label(requests: List<LabelRequest>): Map<String, String>?
}
