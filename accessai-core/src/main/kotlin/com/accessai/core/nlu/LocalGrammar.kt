package com.accessai.core.nlu

/** Built-in commands the SDK recognises on-device, without the network. */
public enum class Control(public val wireName: String) {
    CANCEL("cancel"),
    STOP("stop"),
    REPEAT("repeat"),
    NEXT("next"),
    PREVIOUS("previous"),
    HELP("help"),
    DESCRIBE_SCREEN("describe_screen");

    public companion object {
        public fun fromWire(name: String?): Control? = entries.firstOrNull { it.wireName == name }
    }
}

/**
 * Deterministic phrase matching. Confirmation in particular never goes through an AI model:
 * only an exact, whole-utterance match counts, so "don't confirm" or "yes" can never confirm.
 */
public object LocalGrammar {

    private val CONTROLS: Map<String, Control> = buildMap {
        listOf("cancel", "cancel it", "cancel that", "cancel transfer", "cancel the transfer", "abort",
            "never mind", "nevermind", "forget it", "stop transfer", "don't send", "do not send")
            .forEach { put(it, Control.CANCEL) }
        listOf("stop", "stop reading", "stop talking", "stop speaking", "quiet", "be quiet", "enough", "that's enough", "thats enough")
            .forEach { put(it, Control.STOP) }
        listOf("repeat", "repeat that", "say that again", "say it again", "again", "pardon", "come again", "what did you say")
            .forEach { put(it, Control.REPEAT) }
        listOf("next", "next one", "next transaction", "next item", "go on", "continue", "read next")
            .forEach { put(it, Control.NEXT) }
        listOf("previous", "previous one", "previous transaction", "go back one")
            .forEach { put(it, Control.PREVIOUS) }
        listOf("help", "what can i do", "what can you do", "what can i say", "options", "menu")
            .forEach { put(it, Control.HELP) }
        listOf("where am i", "describe screen", "describe the screen", "describe this screen", "read screen",
            "read the screen", "what is on the screen", "what's on the screen", "whats on the screen", "what is on this screen")
            .forEach { put(it, Control.DESCRIBE_SCREEN) }
    }

    private val BARE_AFFIRMATIVES = setOf(
        "yes", "yeah", "yep", "yes please", "ok", "okay", "sure", "go ahead", "do it", "send it", "proceed", "correct",
        "that's right", "thats right", "right", "alright", "fine", "send", "confirmed",
    )

    private val POLITE_PREFIXES = listOf("please ", "abeg ", "ok ", "okay ", "um ", "uh ", "so ")
    private val POLITE_SUFFIXES = listOf(" please", " now", " abeg")

    /** Lowercase, strip punctuation, collapse whitespace, drop filler like "please". */
    public fun normalize(text: String): String {
        var s = text.lowercase()
            .replace('’', '\'')
            .replace(Regex("[^\\p{L}\\p{N}'₦ ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        var changed = true
        while (changed) {
            changed = false
            for (p in POLITE_PREFIXES) if (s.startsWith(p) && s.length > p.length) { s = s.removePrefix(p); changed = true }
            for (p in POLITE_SUFFIXES) if (s.endsWith(p) && s.length > p.length) { s = s.removeSuffix(p); changed = true }
        }
        return s.trim()
    }

    /** A built-in command, only when the whole utterance is that command. */
    public fun control(normalized: String): Control? = CONTROLS[normalized]

    /**
     * True only for an exact confirmation: "confirm", "I confirm", "yes confirm", "confirm it",
     * or the action's own phrase such as "confirm transfer" (optionally prefixed by "yes" / "I").
     */
    public fun isConfirm(normalized: String, actionPhrase: String): Boolean {
        val phrase = normalize(actionPhrase)
        val accepted = setOf("confirm", "i confirm", "yes confirm", "confirm it", phrase, "yes $phrase", "i $phrase")
        return normalized in accepted
    }

    /** "yes", "okay"… — agreement that is deliberately not enough to confirm a sensitive action. */
    public fun isBareAffirmative(normalized: String): Boolean = normalized in BARE_AFFIRMATIVES

    private val ORDINALS = mapOf(
        "first" to 0, "1st" to 0, "number one" to 0, "the first" to 0, "the first one" to 0, "first one" to 0,
        "second" to 1, "2nd" to 1, "number two" to 1, "the second" to 1, "the second one" to 1, "second one" to 1,
        "third" to 2, "3rd" to 2, "number three" to 2, "the third" to 2, "the third one" to 2, "third one" to 2,
        "fourth" to 3, "4th" to 3, "number four" to 3, "the fourth one" to 3, "fourth one" to 3,
        "fifth" to 4, "5th" to 4, "number five" to 4, "the fifth one" to 4, "fifth one" to 4,
    )

    private val PICK_STOPWORDS = setOf("the", "one", "to", "send", "it", "i", "mean", "want", "is", "mr", "mrs", "miss", "ms", "dr", "for", "my", "account")

    /**
     * Picks one of [labels] from a reply like "Adeyemi", "David Okafor" or "the second one".
     * Returns the index only when exactly one label matches.
     */
    public fun pickCandidate(normalized: String, labels: List<String>): Int? {
        ORDINALS[normalized]?.let { return it.takeIf { i -> i < labels.size } }
        val words = normalized.split(' ').filter { it.isNotBlank() && it !in PICK_STOPWORDS }
        if (words.isEmpty()) return null
        val matches = labels.indices.filter { i ->
            val labelWords = normalize(labels[i]).split(' ').toSet()
            words.all { it in labelWords }
        }
        return matches.singleOrNull()
    }
}
