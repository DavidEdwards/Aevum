package dae.aevum.database.entities

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import dae.aevum.utils.IssueId
import java.time.Instant

@Keep
@Entity(
    tableName = "worklogs",
    indices = [
        Index(value = ["userId", "jiraWorklogId"], unique = true),
    ],
)
data class WorklogEntity(
    @PrimaryKey(autoGenerate = true)
    val workId: Long,
    /** Jira remote worklog id when synced; null for local-only pending rows. */
    val jiraWorklogId: Long? = null,
    val issueId: IssueId,
    val userId: Int,
    val jiraUserAccountId: String,
    val from: Instant,
    val to: Instant,
    val author: String,
    val summary: String,
    val pending: Boolean,
    val active: Boolean,
)
