package com.leathalenterprises.onyx.data

/**
 * The safety net between a [Labeler]'s proposals and the home screen: any
 * proposal that is missing, malformed, or collides with another label falls
 * back to the app's real name. The worst possible labeling outcome is
 * "nothing changed".
 */
object LabelValidator {

    private const val MAX_LABEL_LENGTH = 12

    /** Returns the final label for every request, keyed by package name. */
    fun finalize(
        requests: List<LabelRequest>,
        proposals: Map<String, String>?,
    ): Map<String, String> {
        val cleaned = requests.associate { request ->
            val proposal = proposals?.get(request.packageName)?.trim()
            request.packageName to
                if (proposal != null && isValid(proposal)) proposal
                else request.originalLabel
        }

        // Two apps may not share a label: generic collisions ("Rides" for
        // both Uber and Lyft) revert to brand names. Compared in canonical
        // form so near-misses like "Map"/"Maps" still count as collisions.
        val collisions = cleaned.values
            .groupingBy { canonical(it) }
            .eachCount()
            .filterValues { it > 1 }
            .keys

        return requests.associate { request ->
            val label = cleaned.getValue(request.packageName)
            request.packageName to
                if (canonical(label) in collisions && label != request.originalLabel) {
                    request.originalLabel
                } else {
                    label
                }
        }
    }

    private fun canonical(label: String): String =
        label.lowercase().removeSuffix("s")

    private fun isValid(label: String): Boolean =
        label.isNotEmpty() &&
            label.length <= MAX_LABEL_LENGTH &&
            label.none { it.isWhitespace() }
}
