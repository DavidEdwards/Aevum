package dae.aevum.wear

import java.util.concurrent.TimeUnit

internal fun formatElapsed(startedAtEpochMs: Long): String {
    val elapsedMs = (System.currentTimeMillis() - startedAtEpochMs).coerceAtLeast(0L)
    val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(elapsedMs)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) {
        "%d:%02d".format(hours, minutes)
    } else {
        "%dm".format(minutes)
    }
}
