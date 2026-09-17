package dae.aevum.wear.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ActiveTimerStoreTest {
    @Test
    fun fromDataMap_inactive_returnsNull() {
        assertNull(
            ActiveTimerStore.fromDataMap(
                active = false,
                issueId = "ABC-1",
                issueTitle = "Title",
                startedAtEpochMs = 1L,
                worklogId = 9L,
            ),
        )
    }

    @Test
    fun fromDataMap_blankIssue_returnsNull() {
        assertNull(
            ActiveTimerStore.fromDataMap(
                active = true,
                issueId = "  ",
                issueTitle = "Title",
                startedAtEpochMs = 1L,
                worklogId = 9L,
            ),
        )
    }

    @Test
    fun fromDataMap_missingWorklog_returnsNull() {
        assertNull(
            ActiveTimerStore.fromDataMap(
                active = true,
                issueId = "ABC-1",
                issueTitle = "Title",
                startedAtEpochMs = 1L,
                worklogId = 0L,
            ),
        )
    }

    @Test
    fun fromDataMap_active_returnsSnapshot() {
        val snapshot =
            ActiveTimerStore.fromDataMap(
                active = true,
                issueId = "ABC-1",
                issueTitle = "Fix tile",
                startedAtEpochMs = 42L,
                worklogId = 99L,
            )
        assertEquals("ABC-1", snapshot?.issueId)
        assertEquals("Fix tile", snapshot?.issueTitle)
        assertEquals(42L, snapshot?.startedAtEpochMs)
        assertEquals(99L, snapshot?.worklogId)
    }
}
