package dae.aevum.database.entities.composites

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import dae.aevum.utils.IssueId
import java.time.Instant

@Keep
@Entity(tableName = "worklogs")
data class WorklogIssue(
    @PrimaryKey(autoGenerate = true)
    val workId: Long,
    val issueId: IssueId,
    val userId: Int,
    val jiraUserAccountId: String,
    val from: Instant,
    val to: Instant,
    val author: String,
    val summary: String,
    val pending: Boolean,
    val active: Boolean,
    var issueTitle: String,
)
