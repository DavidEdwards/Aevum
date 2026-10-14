package dae.aevum.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalNetworkUrlsTest {
    @Test
    fun detectsPrivateIpv4AndLocalhost() {
        assertTrue(LocalNetworkUrls.looksLikeLocalNetworkUrl("http://192.168.1.9:9151/v1"))
        assertTrue(LocalNetworkUrls.looksLikeLocalNetworkUrl("http://10.0.0.5:11434"))
        assertTrue(LocalNetworkUrls.looksLikeLocalNetworkUrl("http://172.16.1.1/v1"))
        assertTrue(LocalNetworkUrls.looksLikeLocalNetworkUrl("http://localhost:11434/api/chat"))
        assertTrue(LocalNetworkUrls.looksLikeLocalNetworkUrl("http://printer.local"))
    }

    @Test
    fun rejectsPublicHosts() {
        assertFalse(LocalNetworkUrls.looksLikeLocalNetworkUrl("https://api.openai.com/v1"))
        assertFalse(LocalNetworkUrls.looksLikeLocalNetworkUrl("https://openrouter.ai/api/v1"))
        assertFalse(LocalNetworkUrls.looksLikeLocalNetworkUrl("https://api.groq.com/openai/v1"))
    }
}
