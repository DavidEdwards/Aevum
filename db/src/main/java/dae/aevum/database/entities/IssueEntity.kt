package dae.aevum.database.entities

import androidx.annotation.Keep
import androidx.room.Entity
import dae.aevum.utils.IssueId

@Keep
@Entity(tableName = "issues", primaryKeys = ["id", "userId"])
data class IssueEntity(
    val id: IssueId,
    val userId: Int,
    val title: String,
    val pinned: Boolean,
    val sort: Int
)
