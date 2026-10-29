package com.accessai.core.nlu

/**
 * Parses naira amounts from text into kobo: "5k", "₦5,000", "N5000", "5,000.50", "five thousand naira",
 * "two thousand five hundred", "1.5m". Returns null when there is no amount or more than one.
 */
public object AmountParser {

    private val NUMERIC = Regex(
        """(?<![\p{L}\d])(?:₦|ngn\s?|n(?=\d))?\s*(\d{1,3}(?:,\d{3})+|\d+)(?:\.(\d{1,2}))?\s*(k|m|thousand|million)?(?![\p{L}\d])""",
        RegexOption.IGNORE_CASE,
    )

    private val SMALL = mapOf(
        "zero" to 0L, "one" to 1L, "two" to 2L, "three" to 3L, "four" to 4L, "five" to 5L, "six" to 6L, "seven" to 7L,
        "eight" to 8L, "nine" to 9L, "ten" to 10L, "eleven" to 11L, "twelve" to 12L, "thirteen" to 13L,
        "fourteen" to 14L, "fifteen" to 15L, "sixteen" to 16L, "seventeen" to 17L, "eighteen" to 18L, "nineteen" to 19L,
        "twenty" to 20L, "thirty" to 30L, "forty" to 40L, "fifty" to 50L, "sixty" to 60L, "seventy" to 70L,
        "eighty" to 80L, "ninety" to 90L, "a" to 1L,
    )
    private val SCALES = mapOf("thousand" to 1_000L, "million" to 1_000_000L, "billion" to 1_000_000_000L)

    public fun parseKobo(text: String): Long? {
        val lower = text.lowercase()
        val numeric = NUMERIC.findAll(lower).map { m ->
            val whole = m.groupValues[1].replace(",", "").toLongOrNull() ?: return@map null
            val fraction = m.groupValues[2].padEnd(2, '0').ifEmpty { "00" }.toLong()
            val multiplier = when (m.groupValues[3]) {
                "k", "thousand" -> 1_000L
                "m", "million" -> 1_000_000L
                else -> 1L
            }
            // "1.5k" = 1,500 naira: apply the multiplier to the fractional part too.
            (whole * 100 + fraction) * multiplier
        }.toList()
        val words = parseWords(lower)
        val all = numeric.filterNotNull() + listOfNotNull(words)
        if (numeric.any { it == null }) return null
        return all.singleOrNull()?.takeIf { it > 0 }
    }

    /** Longest run of number words, e.g. "five thousand two hundred and fifty" → 525000 kobo. */
    private fun parseWords(lower: String): Long? {
        val tokens = lower.replace('-', ' ').split(Regex("[^a-z]+")).filter { it.isNotBlank() }
        val runs = mutableListOf<Long>()
        var total = 0L
        var current = 0L
        var inRun = false
        var sawNumber = false
        fun flush() {
            if (inRun && sawNumber) runs += (total + current)
            total = 0; current = 0; inRun = false; sawNumber = false
        }
        for (t in tokens) {
            when {
                t == "a" && !inRun -> { inRun = true; current = 1 } // "a thousand"
                t in SMALL && t != "a" -> { inRun = true; sawNumber = true; current += SMALL.getValue(t) }
                t == "hundred" && inRun -> { current = maxOf(current, 1) * 100; sawNumber = true }
                t in SCALES && inRun -> { total += maxOf(current, 1) * SCALES.getValue(t); current = 0; sawNumber = true }
                t == "and" && inRun -> Unit
                else -> flush()
            }
        }
        flush()
        return runs.singleOrNull()?.let { it * 100 }
    }
}
