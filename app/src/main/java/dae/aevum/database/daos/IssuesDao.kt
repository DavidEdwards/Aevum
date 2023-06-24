package dae.aevum.database.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dae.aevum.database.entities.ActiveIssueEntity
import dae.aevum.database.entities.IssueEntity
import dae.aevum.database.entities.WorklogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IssuesDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addIssue(entity: IssueEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addIssues(entity: List<IssueEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addIssuesSafely(entity: List<IssueEntity>)

    @Query("UPDATE issues SET sort = 1000 WHERE userId = :userId")
    suspend fun removeSorting(userId: Int)

    @Query("UPDATE issues SET sort = :sort WHERE id = :issueId AND userId = :userId")
    suspend fun updateSortingFor(userId: Int, issueId: String, sort: Int)

    @Query("SELECT * FROM issues WHERE id = :issueId AND userId = :userId")
    suspend fun findIssueById(userId: Int, issueId: String): IssueEntity

    @Query("SELECT * FROM issues WHERE id = :issueId AND userId = :userId")
    fun flowIssueById(userId: Int, issueId: String): Flow<IssueEntity>

    @Query("SELECT * FROM worklogs WHERE workId = :worklogId AND userId = :userId")
    fun flowWorklogById(userId: Int, worklogId: Long): Flow<WorklogEntity?>

    @Query("SELECT * FROM issues, worklogs WHERE issues.id = worklogs.issueId AND worklogs.active = 1 AND issues.userId = :userId LIMIT 1")
    fun flowActiveIssue(userId: Int): Flow<ActiveIssueEntity?>

    @Query("SELECT * FROM issues WHERE userId = :userId ORDER BY pinned DESC, sort ASC")
    suspend fun getAllIssues(userId: Int): List<IssueEntity>

    @Query("SELECT * FROM issues WHERE userId = :userId ORDER BY pinned DESC, sort ASC LIMIT 50")
    fun flowAllIssues(userId: Int): Flow<List<IssueEntity>>

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateIssue(entity: IssueEntity)

    @Query("UPDATE issues SET pinned = NOT pinned WHERE id = :issueId AND userId = :userId")
    suspend fun toggleIssuePin(userId: Int, issueId: String)

    @Delete
    suspend fun deleteIssue(entity: IssueEntity)
}