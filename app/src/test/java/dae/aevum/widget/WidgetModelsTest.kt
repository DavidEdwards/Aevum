package dae.aevum.widget

import dae.aevum.database.entities.IssueEntity
import dae.aevum.database.entities.WorklogEntity
import dae.aevum.utils.IssueId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class WidgetModelsTest {
    @Test
    fun formatElapsed_underOneHour_usesMinutes() {
        val start = 1_000_000L
        assertEquals("0m", WidgetFormatters.formatElapsed(start, start))
        assertEquals("5m", WidgetFormatters.formatElapsed(start, start + 5 * 60_000L))
        assertEquals("59m", WidgetFormatters.formatElapsed(start, start + 59 * 60_000L))
    }

    @Test
    fun formatElapsed_overOneHour_usesHMm() {
        val start = 1_000_000L
        assertEquals("1:00", WidgetFormatters.formatElapsed(start, start + 60 * 60_000L))
        assertEquals("1:05", WidgetFormatters.formatElapsed(start, start + 65 * 60_000L))
        assertEquals("2:30", WidgetFormatters.formatElapsed(start, start + 150 * 60_000L))
    }

    @Test
    fun idleResolver_prefersLastCompletedOverPinned() {
        val completed =
            worklog(issueId = "DONE-1", from = Instant.parse("2026-01-01T10:00:00Z"))
        val completedIssue = issue(id = "DONE-1", title = "Done", pinned = false)
        val pinned = issue(id = "PIN-1", title = "Pinned", pinned = true)
        val suggestion =
            WidgetIdleResolver.resolve(
                lastCompleted = completed,
                lastCompletedIssue = completedIssue,
                firstPinned = pinned,
            )
        assertEquals("DONE-1", suggestion?.issueId)
    }

    @Test
    fun idleResolver_fallsBackToPinned() {
        val pinned = issue(id = "PIN-1", title = "Pinned", pinned = true)
        val suggestion =
            WidgetIdleResolver.resolve(
                lastCompleted = null,
                lastCompletedIssue = null,
                firstPinned = pinned,
            )
        assertEquals("PIN-1", suggestion?.issueId)
        assertEquals("Pinned", suggestion?.issueTitle)
    }

    @Test
    fun idleResolver_returnsNullWhenNothingAvailable() {
        assertNull(
            WidgetIdleResolver.resolve(
                lastCompleted = null,
                lastCompletedIssue = null,
                firstPinned = null,
            ),
        )
    }

    private fun issue(
        id: String,
        title: String,
        pinned: Boolean,
    ) = IssueEntity(
        id = IssueId(id),
        userId = 1,
        title = title,
        pinned = pinned,
        sort = 0,
    )

    private fun worklog(
        issueId: String,
        from: Instant,
    ) = WorklogEntity(
        workId = 1L,
        jiraWorklogId = null,
        issueId = IssueId(issueId),
        userId = 1,
        jiraUserAccountId = "account",
        from = from,
        to = from.plusSeconds(3600),
        author = "me",
        summary = "done",
        pending = true,
        active = false,
    )
}
