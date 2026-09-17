package dae.aevum.utils

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

object WearStopTimerCodec {
    fun encode(request: WearStopTimerRequest): ByteArray {
        val summaryBytes = request.summary.toByteArray(StandardCharsets.UTF_8)
        return ByteBuffer
            .allocate(Long.SIZE_BYTES + summaryBytes.size)
            .putLong(request.worklogId)
            .put(summaryBytes)
            .array()
    }

    fun decode(bytes: ByteArray?): WearStopTimerRequest? {
        if (bytes == null || bytes.size < Long.SIZE_BYTES) return null
        return try {
            val buffer = ByteBuffer.wrap(bytes)
            val worklogId = buffer.long
            val remaining = ByteArray(buffer.remaining())
            buffer.get(remaining)
            WearStopTimerRequest(
                worklogId = worklogId,
                summary = String(remaining, StandardCharsets.UTF_8),
            ).takeIf { it.worklogId > 0L }
        } catch (_: Throwable) {
            null
        }
    }
}
