package dae.aevum.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantFeatureTest {
    @Test
    fun normalize_mapsSynonymsToCanonicalFeatures() {
        assertEquals(Intents.FEATURE_ISSUES, AssistantFeature.normalize("Tickets"))
        assertEquals(Intents.FEATURE_WORKLOGS, AssistantFeature.normalize("pending worklogs"))
        assertEquals(Intents.FEATURE_SUMMARY, AssistantFeature.normalize("Timesheet"))
    }

    @Test
    fun normalize_preservesCanonicalIds() {
        assertEquals(Intents.FEATURE_ISSUES, AssistantFeature.normalize("issues"))
        assertEquals(Intents.FEATURE_WORKLOGS, AssistantFeature.normalize("worklogs"))
        assertEquals(Intents.FEATURE_SUMMARY, AssistantFeature.normalize("summary"))
    }

    @Test
    fun normalize_blankOrNullReturnsNull() {
        assertNull(AssistantFeature.normalize(null))
        assertNull(AssistantFeature.normalize("  "))
    }

    @Test
    fun isKnown_onlyAcceptsSupportedFeatures() {
        assertTrue(AssistantFeature.isKnown("tickets"))
        assertFalse(AssistantFeature.isKnown("settings"))
    }
}
