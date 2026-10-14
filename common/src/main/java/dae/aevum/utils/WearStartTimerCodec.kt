package dae.aevum.utils

import java.nio.charset.StandardCharsets

object WearStartTimerCodec {
    fun encode(request: WearStartTimerRequest): ByteArray = request.issueId.trim().toByteArray(StandardCharsets.UTF_8)

    fun decode(bytes: ByteArray?): WearStartTimerRequest? {
        if (bytes == null || bytes.isEmpty()) return null
        val issueId = String(bytes, StandardCharsets.UTF_8).trim()
        return WearStartTimerRequest(issueId = issueId).takeIf { it.issueId.isNotBlank() }
    }
}
