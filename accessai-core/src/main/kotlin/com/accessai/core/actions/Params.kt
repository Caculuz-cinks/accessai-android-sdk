package com.accessai.core.actions

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.longOrNull

public sealed interface ParamType {
    public val wireName: String

    /** Free text, e.g. a recipient name as the user said it. */
    public data object Text : ParamType { override val wireName: String = "string" }

    public data object Integer : ParamType { override val wireName: String = "integer" }

    /** Money in the currency's minor unit (kobo for NGN). Always an integer. */
    public data object AmountMinor : ParamType { override val wireName: String = "amount_minor" }

    public data class OneOf(val values: List<String>) : ParamType { override val wireName: String = "enum" }
}

/** A parameter value. Only strings and integers exist so confirmation hashing stays unambiguous. */
public sealed interface ParamValue {
    public data class Text(val value: String) : ParamValue
    public data class Number(val value: Long) : ParamValue

    public fun toJson(): JsonElement = when (this) {
        is Text -> JsonPrimitive(value)
        is Number -> JsonPrimitive(value)
    }

    public companion object {
        /** Coerces a JSON value from the interpreter to [type]; null if it does not fit. */
        public fun fromJson(element: JsonElement, type: ParamType): ParamValue? {
            val primitive = element as? JsonPrimitive ?: return null
            return when (type) {
                ParamType.Text -> primitive.content.trim().takeIf { primitive.isString && it.isNotEmpty() }?.let(::Text)
                is ParamType.OneOf -> primitive.content.takeIf { primitive.isString && it in type.values }?.let(::Text)
                ParamType.Integer, ParamType.AmountMinor -> {
                    // Reject 5000.5 or "5000": the interpreter must send a JSON integer.
                    if (primitive.isString) null else primitive.longOrNull?.let(::Number)
                }
            }
        }
    }
}

/** What a host [ParamResolver] concludes about a raw value the user gave. */
public sealed interface Resolution {
    /** Exactly one match. [value] replaces the raw value; [label] is how it is read back. */
    public data class Resolved(val value: ParamValue, val label: String, val spokenLabel: String = label) : Resolution

    /** Several matches: AccessAI asks the user to pick, it never guesses. */
    public data class Ambiguous(val candidates: List<Candidate>) : Resolution

    /** No match. [message] is spoken, e.g. "I couldn't find Tunde in your saved beneficiaries." */
    public data class NotFound(val message: String) : Resolution

    public data class Candidate(val value: ParamValue, val label: String, val spokenLabel: String = label)
}

public fun interface ParamResolver {
    public suspend fun resolve(raw: ParamValue): Resolution
}

public sealed interface Validation {
    public data object Valid : Validation
    /** [message] is spoken and the action does not run. */
    public data class Invalid(val message: String) : Validation
}

public class ParamSpec internal constructor(
    public val name: String,
    public val type: ParamType,
    public val description: String,
    public val required: Boolean,
    /** Question asked when this parameter is missing, e.g. "How much would you like to send?" */
    public val prompt: String,
    public val resolver: ParamResolver?,
)
