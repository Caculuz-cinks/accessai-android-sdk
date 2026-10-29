package com.accessai.core.testing

import com.accessai.core.net.AccessAIBackend
import com.accessai.core.net.BackendException
import com.accessai.core.protocol.CancelConfirmationRequest
import com.accessai.core.protocol.Canonical
import com.accessai.core.protocol.ConfirmRequest
import com.accessai.core.protocol.ConfirmResponse
import com.accessai.core.protocol.CreateConfirmationRequest
import com.accessai.core.protocol.CreateConfirmationResponse
import com.accessai.core.protocol.CreateSessionRequest
import com.accessai.core.protocol.CreateSessionResponse
import com.accessai.core.protocol.InterpretRequest
import com.accessai.core.protocol.InterpretResponse

/** A backend that returns whatever the test says, to simulate a misbehaving or unavailable service. */
class ScriptedBackend(
    private val interpretResponse: InterpretResponse? = null,
    private val interpretError: Boolean = false,
    private val confirmationResponse: CreateConfirmationResponse? = null,
) : AccessAIBackend {
    val interpretRequests = mutableListOf<InterpretRequest>()

    override suspend fun createSession(request: CreateSessionRequest) = CreateSessionResponse("s1")

    override suspend fun interpret(request: InterpretRequest): InterpretResponse {
        interpretRequests += request
        if (interpretError) throw BackendException(BackendException.NETWORK, "offline")
        return interpretResponse ?: InterpretResponse(outcome = "unsupported")
    }

    override suspend fun createConfirmation(request: CreateConfirmationRequest): CreateConfirmationResponse =
        confirmationResponse ?: CreateConfirmationResponse("pc_1", Canonical.sha256(emptyMap()), "2100-01-01T00:00:00Z")

    override suspend fun confirm(pendingId: String, request: ConfirmRequest) =
        ConfirmResponse("confirmed", "token", "nonce", "2100-01-01T00:00:00Z")

    override suspend fun cancelConfirmation(pendingId: String, request: CancelConfirmationRequest) = Unit
}
