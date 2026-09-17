package dae.aevum.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WearStopTimerCodecTest {
    @Test
    fun roundTrip_preservesWorklogAndSummary() {
        val request = WearStopTimerRequest(worklogId = 42L, summary = "Fixed the tile stop flow")
        val decoded = WearStopTimerCodec.decode(WearStopTimerCodec.encode(request))
        assertEquals(request, decoded)
    }

    @Test
    fun roundTrip_emptySummary() {
        val request = WearStopTimerRequest(worklogId = 7L, summary = "")
        val decoded = WearStopTimerCodec.decode(WearStopTimerCodec.encode(request))
        assertEquals(request, decoded)
    }

    @Test
    fun decode_rejectsInvalidPayload() {
        assertNull(WearStopTimerCodec.decode(null))
        assertNull(WearStopTimerCodec.decode(ByteArray(0)))
        assertNull(
            WearStopTimerCodec.decode(
                WearStopTimerCodec.encode(WearStopTimerRequest(worklogId = 0L, summary = "x")),
            ),
        )
    }
}
