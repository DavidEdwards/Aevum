package dae.aevum.ui.models

import dae.aevum.database.entities.composites.WorklogIssue
import dae.aevum.utils.IssueId
import java.time.Instant

data class WorklogIssueUiModel(
    val workId: Long,
    val issueId: IssueId,
    val author: String,
    val from: Instant,
    val to: Instant,
    val comment: String,
    val pending: Boolean,
    val active: Boolean,
    val issueTitle: String,
) {
    companion object {
        fun fromEntity(entity: WorklogIssue): WorklogIssueUiModel =
            WorklogIssueUiModel(
                workId = entity.workId,
                issueId = entity.issueId,
                author = entity.author,
                from = entity.from,
                to = entity.to,
                comment = entity.summary,
                pending = entity.pending,
                active = entity.active,
                issueTitle = entity.issueTitle,
            )
    }
}
