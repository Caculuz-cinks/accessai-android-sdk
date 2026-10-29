package com.accessai.core.protocol

import com.accessai.core.actions.ParamValue
import kotlinx.serialization.json.JsonObject
import java.security.MessageDigest

/**
 * Canonical form of confirmed parameters, shared byte-for-byte with the AI service and the host backend:
 * a JSON object with keys sorted, no whitespace, strings as JSON strings, numbers as integers.
 * Python equivalent: json.dumps(params, sort_keys=True, separators=(",", ":"), ensure_ascii=False)
 */
public object Canonical {

    public fun json(params: Map<String, ParamValue>): String =
        JsonObject(params.toSortedMap().mapValues { it.value.toJson() }).toString()

    public fun jsonObject(params: Map<String, ParamValue>): JsonObject =
        JsonObject(params.toSortedMap().mapValues { it.value.toJson() })

    /** Lowercase hex SHA-256 of [json]. */
    public fun sha256(params: Map<String, ParamValue>): String =
        MessageDigest.getInstance("SHA-256")
            .digest(json(params).toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
