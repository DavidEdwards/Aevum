package dae.aevum.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WearStartTimerCodecTest {
    @Test
    fun roundTrip_preservesIssueId() {
        val request = WearStartTimerRequest(issueId = "AAPP-42")
        val decoded = WearStartTimerCodec.decode(WearStartTimerCodec.encode(request))
        assertEquals(request, decoded)
    }

    @Test
    fun decode_rejectsBlank() {
        assertNull(WearStartTimerCodec.decode(null))
        assertNull(WearStartTimerCodec.decode(ByteArray(0)))
        assertNull(WearStartTimerCodec.decode("   ".toByteArray()))
    }
}
