package dae.aevum.widget

import dae.aevum.database.entities.IssueEntity
import dae.aevum.database.entities.WorklogEntity
import java.util.concurrent.TimeUnit

data class WidgetActiveState(
    val worklogId: Long,
    val issueId: String,
    val issueTitle: String,
    val startedAtEpochMs: Long,
)

data class WidgetIdleState(
    val issueId: String,
    val issueTitle: String,
)

object WidgetFormatters {
    fun formatElapsed(
        startedAtEpochMs: Long,
        nowEpochMs: Long = System.currentTimeMillis(),
    ): String {
        val elapsedMs = (nowEpochMs - startedAtEpochMs).coerceAtLeast(0L)
        val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(elapsedMs)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return if (hours > 0) {
            "%d:%02d".format(hours, minutes)
        } else {
            "%dm".format(minutes)
        }
    }
}

object WidgetIdleResolver {
    /**
     * Prefers the issue from the last completed worklog; falls back to the first pinned issue.
     */
    fun resolve(
        lastCompleted: WorklogEntity?,
        lastCompletedIssue: IssueEntity?,
        firstPinned: IssueEntity?,
    ): WidgetIdleState? {
        if (lastCompleted != null && lastCompletedIssue != null) {
            return WidgetIdleState(
                issueId = lastCompletedIssue.id.value,
                issueTitle = lastCompletedIssue.title,
            )
        }
        if (firstPinned != null) {
            return WidgetIdleState(
                issueId = firstPinned.id.value,
                issueTitle = firstPinned.title,
            )
        }
        return null
    }
}
