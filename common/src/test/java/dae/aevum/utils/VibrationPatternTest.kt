package dae.aevum.utils

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VibrationPatternTest {
    @Test
    fun triple_hasThreeOnPulses() {
        val timings = VibrationPattern.TRIPLE.timings(200L)
        // off, on, off, on, off, on
        assertEquals(6, timings.size)
        assertEquals(0L, timings[0])
        assertEquals(200L, timings[1])
        assertEquals(200L, timings[3])
        assertEquals(200L, timings[5])
    }

    @Test
    fun shortLongShort_middlePulseIsLonger() {
        val timings = VibrationPattern.SHORT_LONG_SHORT.timings(200L)
        assertEquals(6, timings.size)
        assertTrue(timings[3] > timings[1])
        assertEquals(timings[1], timings[5])
    }

    @Test
    fun single_isDelayThenPulse() {
        assertArrayEquals(
            longArrayOf(0L, 320L),
            VibrationPattern.SINGLE.timings(320L),
        )
    }

    @Test
    fun fromStored_defaultsToTriple() {
        assertEquals(VibrationPattern.TRIPLE, VibrationPattern.fromStored(null))
        assertEquals(
            VibrationPattern.HEARTBEAT,
            VibrationPattern.fromStored("HEARTBEAT"),
        )
    }
}
