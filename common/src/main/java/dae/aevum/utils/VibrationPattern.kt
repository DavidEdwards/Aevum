package dae.aevum.utils

import android.os.VibrationEffect

/**
 * Recognizable haptic patterns for the active-timer reminder.
 * [timings] scales short/long pulses from [shortPulseMs].
 */
enum class VibrationPattern {
    /** One pulse. */
    SINGLE,

    /** Two short pulses. */
    DOUBLE,

    /** Three short pulses. */
    TRIPLE,

    /** Short – long – short. */
    SHORT_LONG_SHORT,

    /** Long – short – long. */
    LONG_SHORT_LONG,

    /** Two quick pairs (ba-bump, ba-bump). */
    HEARTBEAT,
    ;

    /**
     * Waveform timings for [VibrationEffect.createWaveform]:
     * off, on, off, on, … (first value is delay before the first buzz).
     */
    fun timings(shortPulseMs: Long): LongArray {
        val short = shortPulseMs.coerceIn(MIN_SHORT_MS, MAX_SHORT_MS)
        val gap = (short * 3 / 5).coerceAtLeast(MIN_GAP_MS)
        val long = (short * 5 / 2).coerceAtLeast(short + MIN_GAP_MS)
        val longGap = (short * 6 / 5).coerceAtLeast(gap)
        val tightGap = (gap / 2).coerceAtLeast(MIN_GAP_MS)
        return when (this) {
            SINGLE -> longArrayOf(0L, short)
            DOUBLE -> longArrayOf(0L, short, gap, short)
            TRIPLE -> longArrayOf(0L, short, gap, short, gap, short)
            SHORT_LONG_SHORT -> longArrayOf(0L, short, gap, long, gap, short)
            LONG_SHORT_LONG -> longArrayOf(0L, long, gap, short, gap, long)
            HEARTBEAT ->
                longArrayOf(
                    0L,
                    short,
                    tightGap,
                    short,
                    longGap,
                    short,
                    tightGap,
                    short,
                )
        }
    }

    companion object {
        private const val MIN_SHORT_MS = 50L
        private const val MAX_SHORT_MS = 2_000L
        private const val MIN_GAP_MS = 40L

        fun fromStored(value: String?): VibrationPattern =
            entries.firstOrNull { it.name == value } ?: TRIPLE
    }
}

object TimerReminderVibration {
    /**
     * Builds a one-shot or waveform effect. [amplitude] of
     * [VibrationEffect.DEFAULT_AMPLITUDE] / negative uses the platform default waveform.
     */
    fun createEffect(
        timings: LongArray,
        amplitude: Int,
    ): VibrationEffect {
        if (timings.size <= 2) {
            val duration = timings.getOrElse(1) { 320L }.coerceAtLeast(1L)
            val amp =
                amplitude.takeIf { it > 0 } ?: VibrationEffect.DEFAULT_AMPLITUDE
            return VibrationEffect.createOneShot(duration, amp)
        }
        return if (amplitude <= 0) {
            VibrationEffect.createWaveform(timings, -1)
        } else {
            val amplitudes =
                IntArray(timings.size) { index ->
                    if (index % 2 == 0) 0 else amplitude
                }
            VibrationEffect.createWaveform(timings, amplitudes, -1)
        }
    }
}
