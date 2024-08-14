package dae.aevum.ui.models

import dae.aevum.database.entities.WorklogEntity
import dae.aevum.utils.IssueId
import java.time.Instant

data class WorklogUiModel(
    val workId: Long,
    val issueId: IssueId,
    val author: String,
    val from: Instant,
    val to: Instant,
    val comment: String,
    val pending: Boolean,
    val active: Boolean
) {
    companion object {
        fun fromEntity(entity: WorklogEntity): WorklogUiModel {
            return WorklogUiModel(
                workId = entity.workId,
                issueId = entity.issueId,
                author = entity.author,
                from = entity.from,
                to = entity.to,
                comment = entity.summary,
                pending = entity.pending,
                active = entity.active
            )
        }
    }
}