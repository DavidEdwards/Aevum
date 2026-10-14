package dae.aevum.utils

import dae.aevum.common.R
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class NetworkErrorsTest {
    @Test
    fun unknownHost_mapsToUnreachable() {
        assertEquals(
            R.string.network_unreachable,
            NetworkErrors.messageRes(UnknownHostException("borrowbox.atlassian.net")),
        )
    }

    @Test
    fun nestedUnknownHost_mapsToUnreachable() {
        assertEquals(
            R.string.network_unreachable,
            NetworkErrors.messageRes(IOException(UnknownHostException("host"))),
        )
    }

    @Test
    fun timeout_mapsToTimeout() {
        assertEquals(
            R.string.network_timeout,
            NetworkErrors.messageRes(SocketTimeoutException("timeout")),
        )
    }

    @Test
    fun connect_mapsToUnreachable() {
        assertEquals(
            R.string.network_unreachable,
            NetworkErrors.messageRes(ConnectException("refused")),
        )
    }

    @Test
    fun other_mapsToRefreshFailed() {
        assertEquals(
            R.string.refresh_failed,
            NetworkErrors.messageRes(IllegalStateException("boom")),
        )
    }
}
