package com.accessai.core.format

/** Formats amounts held in kobo (1 naira = 100 kobo). Money never passes through floating point. */
public object Naira {

    /** 8425000 → "₦84,250"; 8425050 → "₦84,250.50". */
    public fun display(kobo: Long): String {
        val sign = if (kobo < 0) "-" else ""
        val abs = Math.abs(kobo)
        val naira = abs / 100
        val rest = abs % 100
        val grouped = naira.toString().reversed().chunked(3).joinToString(",").reversed()
        return if (rest == 0L) "$sign₦$grouped" else "$sign₦$grouped.${rest.toString().padStart(2, '0')}"
    }

    /** 500000 → "five thousand naira"; 150 → "one naira and fifty kobo". */
    public fun spoken(kobo: Long): String {
        val prefix = if (kobo < 0) "minus " else ""
        val abs = Math.abs(kobo)
        val naira = abs / 100
        val rest = abs % 100
        return when {
            naira == 0L && rest == 0L -> "zero naira"
            rest == 0L -> "$prefix${NumberWords.toWords(naira)} naira"
            naira == 0L -> "$prefix${NumberWords.toWords(rest)} kobo"
            else -> "$prefix${NumberWords.toWords(naira)} naira and ${NumberWords.toWords(rest)} kobo"
        }
    }
}

public object NumberWords {
    private val ones = listOf(
        "zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten",
        "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen",
    )
    private val tens = listOf("", "", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety")
    private val scales = listOf(1_000_000_000_000L to "trillion", 1_000_000_000L to "billion", 1_000_000L to "million", 1_000L to "thousand")

    public fun toWords(value: Long): String {
        require(value >= 0) { "negative" }
        if (value < 1000) return underThousand(value.toInt())
        for ((scale, name) in scales) {
            if (value >= scale) {
                val head = toWords(value / scale) + " " + name
                val tail = value % scale
                return when {
                    tail == 0L -> head
                    tail < 100 -> "$head and ${underThousand(tail.toInt())}"
                    else -> "$head ${toWords(tail)}"
                }
            }
        }
        error("unreachable")
    }

    private fun underThousand(n: Int): String = when {
        n < 20 -> ones[n]
        n < 100 -> tens[n / 10] + if (n % 10 != 0) "-" + ones[n % 10] else ""
        else -> ones[n / 100] + " hundred" + if (n % 100 != 0) " and " + underThousand(n % 100) else ""
    }
}
