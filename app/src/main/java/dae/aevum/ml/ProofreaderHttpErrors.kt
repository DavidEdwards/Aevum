package dae.aevum.ml

import dae.aevum.common.R
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.SocketTimeoutException

internal object ProofreaderHttpErrors {
    fun logRes(error: Throwable): Int =
        if (isTimeout(error)) {
            R.string.proofreader_log_timeout
        } else {
            R.string.proofreader_log_connect_failed
        }

    fun isTimeout(error: Throwable): Boolean {
        if (error is SocketTimeoutException || error.cause is SocketTimeoutException) return true
        // OkHttp callTimeout surfaces as InterruptedIOException("timeout"), not SocketTimeoutException.
        return isInterruptedTimeout(error) || isInterruptedTimeout(error.cause)
    }

    fun isConnectFailure(error: Throwable): Boolean =
        error is ConnectException ||
            error.cause is ConnectException ||
            // Connect-phase timeouts only — not callTimeout while waiting for the model.
            isConnectTimeout(error) ||
            isConnectTimeout(error.cause)

    private fun isInterruptedTimeout(error: Throwable?): Boolean =
        error is InterruptedIOException &&
            error !is SocketTimeoutException &&
            error.message?.contains("timeout", ignoreCase = true) == true

    private fun isConnectTimeout(error: Throwable?): Boolean =
        error is SocketTimeoutException &&
            error.message?.contains("connect", ignoreCase = true) == true
}
