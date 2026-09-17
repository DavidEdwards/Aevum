package dae.aevum.database.daos

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.RoomWarnings
import androidx.room.Transaction
import androidx.room.Update
import dae.aevum.database.entities.ActiveIssueEntity
import dae.aevum.database.entities.IssueEntity
import dae.aevum.database.entities.composites.WorklogIssue
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
    suspend fun updateSortingFor(
        userId: Int,
        issueId: String,
        sort: Int,
    )

    @Query("SELECT * FROM issues WHERE id = :issueId AND userId = :userId")
    suspend fun findIssueById(
        userId: Int,
        issueId: String,
    ): IssueEntity

    @Query("SELECT COUNT(*) FROM issues WHERE id = :issueId AND userId = :userId")
    suspend fun issueExists(
        userId: Int,
        issueId: String,
    ): Int

    @Query("SELECT * FROM issues WHERE id = :issueId AND userId = :userId")
    fun flowIssueById(
        userId: Int,
        issueId: String,
    ): Flow<IssueEntity>

    @Query(
        "SELECT worklogs.*, issues.title AS issueTitle FROM issues, worklogs WHERE issues.id = worklogs.issueId AND issues.userId = worklogs.userId AND worklogs.workId = :worklogId AND issues.userId = :userId",
    )
    fun flowWorklogById(
        userId: Int,
        worklogId: Long,
    ): Flow<WorklogIssue?>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @RewriteQueriesToDropUnusedColumns
    @Transaction
    @Query("SELECT * FROM issues, worklogs WHERE issues.id = worklogs.issueId AND worklogs.active = 1 AND issues.userId = :userId AND worklogs.userId = :userId LIMIT 1")
    fun flowActiveIssue(userId: Int): Flow<ActiveIssueEntity?>

    @Query("SELECT * FROM issues WHERE userId = :userId ORDER BY sort ASC")
    suspend fun getAllIssues(userId: Int): List<IssueEntity>

    @Query("SELECT * FROM issues WHERE userId = :userId ORDER BY sort ASC LIMIT 500")
    fun flowAllIssues(userId: Int): Flow<List<IssueEntity>>

    @Query("SELECT * FROM issues WHERE userId = :userId ORDER BY sort ASC")
    fun pagingSource(userId: Int): PagingSource<Int, IssueEntity>

    @Query("SELECT * FROM issues WHERE userId = :userId AND pinned = 1 ORDER BY sort ASC")
    fun flowPinnedIssues(userId: Int): Flow<List<IssueEntity>>

    @Query(
        "SELECT * FROM issues WHERE userId = :userId AND pinned = 1 " +
            "ORDER BY sort ASC LIMIT 1",
    )
    suspend fun getFirstPinnedIssue(userId: Int): IssueEntity?

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateIssue(entity: IssueEntity)

    @Query("UPDATE issues SET pinned = NOT pinned WHERE id = :issueId AND userId = :userId")
    suspend fun toggleIssuePin(
        userId: Int,
        issueId: String,
    )

    @Delete
    suspend fun deleteIssue(entity: IssueEntity)

    @Query("DELETE FROM issues WHERE userId = :userId")
    suspend fun deleteIssuesForUser(userId: Int): Int

    @Transaction
    suspend fun refreshWith(
        userId: Int,
        entityList: List<IssueEntity>,
    ) {
        removeSorting(userId)
        addIssuesSafely(entityList)

        entityList.forEach {
            updateSortingFor(userId, it.id.value, it.sort)
        }
    }
}
