package com.asulcons.embedded

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Closest-match lookup shared by the "unknown mnemonic / directive / command / region" inspections.
 *
 * The distance is a plain Levenshtein edit distance computed on two rolling rows, which is more than
 * fast enough for the candidate sets involved here (a few thousand entries at most) and avoids pulling
 * in a platform utility whose signature has churned across releases.
 */
object Suggestions {

    /** Never propose a replacement that shares almost nothing with what the user typed. */
    private const val MAX_RELATIVE_DISTANCE = 0.34

    fun closestMatches(input: String, candidates: Collection<String>, limit: Int = 3): List<String> {
        if (input.isEmpty()) return emptyList()
        val budget = max(1, (input.length * MAX_RELATIVE_DISTANCE).toInt())
        return candidates.asSequence()
            .filter { abs(it.length - input.length) <= budget }
            .map { it to distance(input, it, budget) }
            .filter { it.second <= budget }
            .sortedWith(compareBy({ it.second }, { it.first }))
            .take(limit)
            .map { it.first }
            .toList()
    }

    /**
     * Damerau-Levenshtein distance (optimal string alignment), aborting as soon as every cell in a row
     * exceeds [budget] so that far-apart candidates cost only a couple of rows instead of a full matrix.
     *
     * Counting a transposition as one edit rather than two is what makes this useful at these budgets:
     * `mvo` is one slip away from `mov` and `SRMA` from `SRAM`, but plain Levenshtein scores both at 2
     * and a three- or four-letter name only affords a budget of 1.
     */
    private fun distance(a: String, b: String, budget: Int): Int {
        var beforePrevious = IntArray(b.length + 1)
        var previous = IntArray(b.length + 1) { it }
        var current = IntArray(b.length + 1)

        for (i in 1..a.length) {
            current[0] = i
            var rowMin = current[0]
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                var best = min(previous[j - 1] + cost, min(previous[j] + 1, current[j - 1] + 1))
                if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) {
                    best = min(best, beforePrevious[j - 2] + 1)
                }
                current[j] = best
                rowMin = min(rowMin, best)
            }
            if (rowMin > budget) return budget + 1

            val recycled = beforePrevious
            beforePrevious = previous
            previous = current
            current = recycled
        }
        return previous[b.length]
    }
}
