package dae.aevum.wear.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecentIssuesStoreTest {
    @Test
    fun fromDataMap_empty_returnsEmpty() {
        assertTrue(RecentIssuesStore.fromDataMap(null, null).isEmpty())
        assertTrue(RecentIssuesStore.fromDataMap(emptyArray(), emptyArray()).isEmpty())
    }

    @Test
    fun fromDataMap_pairsIdsAndTitles() {
        val issues =
            RecentIssuesStore.fromDataMap(
                ids = arrayOf("A-1", "A-2"),
                titles = arrayOf("First", "Second"),
            )
        assertEquals(2, issues.size)
        assertEquals("A-1", issues[0].issueId)
        assertEquals("First", issues[0].issueTitle)
        assertEquals("A-2", issues[1].issueId)
        assertEquals("Second", issues[1].issueTitle)
    }

    @Test
    fun fromDataMap_missingTitle_usesEmpty() {
        val issues =
            RecentIssuesStore.fromDataMap(
                ids = arrayOf("A-1"),
                titles = emptyArray(),
            )
        assertEquals(1, issues.size)
        assertEquals("", issues[0].issueTitle)
    }
}
