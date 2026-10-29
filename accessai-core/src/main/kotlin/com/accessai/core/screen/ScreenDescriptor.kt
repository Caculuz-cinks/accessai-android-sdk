package com.accessai.core.screen

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A structured description of a supported screen (PRD §8.4). Hosts publish one per screen so
 * AccessAI can describe it and the interpreter knows where the user is.
 */
@Serializable
public data class ScreenDescriptor(
    @SerialName("screen_id") val screenId: String,
    val title: String,
    val summary: String? = null,
    val elements: List<ScreenElement> = emptyList(),
) {
    /** Plain-language description for "where am I" / "describe screen". */
    public fun describe(): String = buildString {
        append(title).append(" screen.")
        summary?.let { append(' ').append(it.trimEnd('.')).append('.') }
        if (elements.isNotEmpty()) {
            append(" It has ")
            append(elements.joinToString("; ") { it.describe() })
            append('.')
        }
    }
}

@Serializable
public data class ScreenElement(
    val id: String,
    /** e.g. "button", "input", "text", "list", "heading". */
    val role: String,
    val label: String,
    val required: Boolean = false,
    val value: String? = null,
) {
    internal fun describe(): String = buildString {
        append(label)
        if (role != "text") append(", ").append(role)
        if (required) append(", required")
        value?.let { append(", ").append(it) }
    }
}
