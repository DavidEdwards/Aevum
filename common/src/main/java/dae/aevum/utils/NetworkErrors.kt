package dae.aevum.utils

import dae.aevum.common.R
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Maps thrown network failures to a short user-facing string resource.
 */
object NetworkErrors {
    fun messageRes(error: Throwable): Int {
        var current: Throwable? = error
        while (current != null) {
            when (current) {
                is UnknownHostException -> return R.string.network_unreachable
                is SocketTimeoutException -> return R.string.network_timeout
                is ConnectException -> return R.string.network_unreachable
            }
            current = current.cause
        }
        return if (error is IOException) {
            R.string.network_unreachable
        } else {
            R.string.refresh_failed
        }
    }
}
