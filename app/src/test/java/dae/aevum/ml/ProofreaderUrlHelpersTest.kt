package dae.aevum.ml

import dae.aevum.domain.preferences.ProofreaderConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class ProofreaderUrlHelpersTest {
    @Test
    fun openAiCompatible_appendsChatCompletions() {
        assertEquals(
            "https://api.openai.com/v1/chat/completions",
            OpenAiCompatibleProofreader.chatCompletionsUrl("https://api.openai.com/v1"),
        )
        assertEquals(
            "https://api.openai.com/v1/chat/completions",
            OpenAiCompatibleProofreader.chatCompletionsUrl("https://api.openai.com"),
        )
        assertEquals(
            "https://api.openai.com/v1/chat/completions",
            OpenAiCompatibleProofreader.chatCompletionsUrl("https://api.openai.com/v1/chat/completions"),
        )
    }

    @Test
    fun ollama_normalizesChatUrl() {
        assertEquals(
            "http://192.168.1.9:11434/api/chat",
            OllamaProofreader.normalizeChatUrl("http://192.168.1.9:11434"),
        )
        assertEquals(
            "http://192.168.1.9:11434/api/chat",
            OllamaProofreader.normalizeChatUrl("http://192.168.1.9:11434/api/chat"),
        )
    }

    @Test
    fun requestTimeout_usesFullValue_connectTimeoutIsCapped() {
        assertEquals(60, ProofreaderConfig.requestTimeoutSeconds(60))
        assertEquals(10, ProofreaderConfig.connectTimeoutSeconds(60))
        // Below min is clamped up, then connect is capped at MAX_CONNECT.
        assertEquals(10, ProofreaderConfig.connectTimeoutSeconds(5))
        assertEquals(
            ProofreaderConfig.MIN_CONNECTION_TIMEOUT_SECONDS,
            ProofreaderConfig.requestTimeoutSeconds(1),
        )
    }

    @Test
    fun httpErrors_treatOkHttpCallTimeoutAsTimeout() {
        val callTimeout = java.io.InterruptedIOException("timeout")
        assertEquals(
            dae.aevum.common.R.string.proofreader_log_timeout,
            ProofreaderHttpErrors.logRes(callTimeout),
        )
        assertEquals(false, ProofreaderHttpErrors.isConnectFailure(callTimeout))
    }
}
