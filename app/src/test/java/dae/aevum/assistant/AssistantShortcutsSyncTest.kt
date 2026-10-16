package dae.aevum.assistant

import dae.aevum.database.entities.IssueEntity
import dae.aevum.utils.IssueId
import dae.aevum.utils.WearRecentIssue
import org.junit.Assert.assertEquals
import org.junit.Test

class AssistantShortcutsSyncTest {
    @Test
    fun mergeCandidates_prefersPinnedThenRecentWithoutDuplicates() {
        val pinned =
            listOf(
                IssueEntity(IssueId("PIN-1"), userId = 1, title = "Pinned", pinned = true, sort = 0),
            )
        val recent =
            listOf(
                WearRecentIssue(issueId = "PIN-1", issueTitle = "Pinned again"),
                WearRecentIssue(issueId = "REC-2", issueTitle = "Recent"),
            )

        val merged = AssistantShortcutsSync.mergeCandidates(pinned, recent)

        assertEquals(listOf("PIN-1", "REC-2"), merged.map { it.issueId })
        assertEquals("Pinned", merged.first().title)
    }

    @Test
    fun shortcutId_isStablePerIssue() {
        assertEquals("start_timer_ABC-9", AssistantShortcutsSync.shortcutId("ABC-9"))
    }
}
