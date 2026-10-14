package dae.aevum.ml

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
}
