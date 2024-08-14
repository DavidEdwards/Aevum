package dae.aevum.database.entities

import androidx.annotation.Keep
import androidx.room.Embedded
import androidx.room.Relation

@Keep
data class ActiveIssueEntity(
    @Embedded val issue: IssueEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "issueId"
    )
    val worklog: WorklogEntity
)
