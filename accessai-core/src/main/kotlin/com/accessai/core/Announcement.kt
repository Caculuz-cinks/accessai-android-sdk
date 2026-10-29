package com.accessai.core

import com.accessai.core.format.Naira

/**
 * Something AccessAI tells the user. [spoken] is optimised for text-to-speech (amounts in words,
 * account numbers digit by digit); [display] is what low-vision users read on screen.
 */
public data class Announcement(
    val spoken: String,
    val display: String = spoken,
) {
    public operator fun plus(other: Announcement): Announcement =
        Announcement(join(spoken, other.spoken), join(display, other.display))

    public companion object {
        public fun of(text: String): Announcement = Announcement(text)

        private fun join(a: String, b: String) = when {
            a.isEmpty() -> b
            b.isEmpty() -> a
            else -> "$a $b"
        }
    }
}

/** Builds an [Announcement] where every money value and identifier is spoken deterministically. */
public fun announce(block: AnnouncementBuilder.() -> Unit): Announcement =
    AnnouncementBuilder().apply(block).build()

public class AnnouncementBuilder internal constructor() {
    private val spoken = StringBuilder()
    private val display = StringBuilder()

    public fun text(value: String): AnnouncementBuilder = apply {
        spoken.append(value)
        display.append(value)
    }

    /** A naira amount given in kobo, e.g. 500000 → "five thousand naira" / "₦5,000". */
    public fun naira(kobo: Long): AnnouncementBuilder = apply {
        spoken.append(Naira.spoken(kobo))
        display.append(Naira.display(kobo))
    }

    /** Digits read one at a time, e.g. "2391" → "2 3 9 1". */
    public fun digits(value: String): AnnouncementBuilder = apply {
        spoken.append(value.filter { it.isDigit() }.toList().joinToString(" "))
        display.append(value)
    }

    /** Different spoken and displayed text, e.g. a date. */
    public fun pair(spokenText: String, displayText: String): AnnouncementBuilder = apply {
        spoken.append(spokenText)
        display.append(displayText)
    }

    internal fun build(): Announcement = Announcement(spoken.toString(), display.toString())
}
