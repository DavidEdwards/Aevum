package dae.aevum.database.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import dae.aevum.database.entities.WorklogEntity
import dae.aevum.database.entities.composites.WorklogIssue
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import java.time.Instant

@Dao
interface WorklogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addWork(entity: WorklogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addWorks(entity: List<WorklogEntity>)

    @Query("SELECT * FROM worklogs WHERE workId = :workId")
    suspend fun findWorkById(workId: Long): WorklogEntity?

    @Query("SELECT * FROM worklogs WHERE issueId = :issueId ORDER BY `from` DESC LIMIT 500")
    fun flowWorkByIssueId(issueId: String): Flow<List<WorklogEntity>>

    @Query("SELECT * FROM worklogs WHERE active = 1 LIMIT 1")
    suspend fun getActiveWorklog(): WorklogEntity?

    @Query("SELECT * FROM worklogs WHERE active = 1 AND userId = :userId LIMIT 1")
    suspend fun getActiveWorklogForUser(userId: Int): WorklogEntity?

    @Query("SELECT * FROM worklogs")
    suspend fun getAllWorks(): List<WorklogEntity>

    @Query("DELETE FROM worklogs WHERE workId = :id")
    suspend fun deleteWorkById(id: Long): Int

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateWork(entity: WorklogEntity)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateWorklogs(entities: List<WorklogEntity>)

    @Delete
    suspend fun deleteWork(entity: WorklogEntity)

    @Query("SELECT * FROM worklogs WHERE userId = :userId AND pending = 1 ORDER BY `from` DESC")
    fun flowPendingWorklogs(userId: Int): Flow<List<WorklogEntity>>

    @Query(
        "SELECT * FROM worklogs WHERE userId = :userId AND ((`from` BETWEEN :dayStart AND :dayEnd) OR (`to` BETWEEN :dayStart AND :dayEnd)) ORDER BY `from` DESC",
    )
    fun flowWorklogsToday(
        userId: Int,
        dayStart: Instant,
        dayEnd: Instant,
    ): Flow<List<WorklogEntity>>

    @Query(
        "SELECT * FROM worklogs WHERE userId = :userId AND jiraUserAccountId = :jiraUserAccountId AND ((`from` BETWEEN :dayStart AND :dayEnd) OR (`to` BETWEEN :dayStart AND :dayEnd)) ORDER BY `from` DESC LIMIT 500",
    )
    fun getWorklogsTodayFromMe(
        userId: Int,
        dayStart: Instant,
        dayEnd: Instant,
        jiraUserAccountId: String,
    ): Flow<List<WorklogEntity>>

    /**
     * Returns all worklogs authored by the current Jira user that overlap with the provided range.
     * Overlap condition uses: from <= end AND to >= start
     */
    @Query(
        "SELECT * FROM worklogs " +
            "WHERE userId = :userId " +
            "AND jiraUserAccountId = :jiraUserAccountId " +
            "AND `from` <= :end " +
            "AND `to` >= :start " +
            "ORDER BY `from` ASC",
    )
    fun getWorklogsFromMeBetween(
        userId: Int,
        start: Instant,
        end: Instant,
        jiraUserAccountId: String,
    ): Flow<List<WorklogEntity>>

    @Query("SELECT worklogs.*, issues.title AS issueTitle FROM worklogs, issues WHERE worklogs.issueId = issues.id")
    fun getWorklogsAndIssues(): Flow<List<WorklogIssue>>

    @Query(
        "SELECT worklogs.*, issues.title AS issueTitle FROM worklogs, issues WHERE worklogs.issueId = issues.id AND worklogs.userId = :userId AND worklogs.jiraUserAccountId = :jiraUserAccountId AND ((worklogs.`from` BETWEEN :dayStart AND :dayEnd) OR (worklogs.`to` BETWEEN :dayStart AND :dayEnd)) ORDER BY worklogs.`from` DESC",
    )
    fun getWorklogIssuesTodayFromMe(
        userId: Int,
        dayStart: Instant,
        dayEnd: Instant,
        jiraUserAccountId: String,
    ): Flow<List<WorklogIssue>>

    @Query(
        "SELECT worklogs.*, issues.title AS issueTitle FROM worklogs, issues " +
            "WHERE worklogs.issueId = issues.id AND worklogs.userId = :userId " +
            "AND worklogs.jiraUserAccountId = :jiraUserAccountId " +
            "AND ((worklogs.`from` BETWEEN :rangeStart AND :rangeEnd) OR (worklogs.`to` BETWEEN :rangeStart AND :rangeEnd)) " +
            "ORDER BY worklogs.`from` DESC",
    )
    fun getWorklogIssuesInRangeFromMe(
        userId: Int,
        rangeStart: Instant,
        rangeEnd: Instant,
        jiraUserAccountId: String,
    ): Flow<List<WorklogIssue>>

    @Transaction
    suspend fun splitWork(
        splitFrom: WorklogEntity,
        splitTo: WorklogEntity,
    ): Long {
        try {
            // Validate inputs before transaction
            if (splitFrom.workId <= 0) {
                throw IllegalArgumentException("Invalid source worklog ID: ${splitFrom.workId}")
            }
            if (splitFrom.from.isAfter(splitFrom.to)) {
                throw IllegalArgumentException("Invalid time range: from ${splitFrom.from} to ${splitFrom.to}")
            }
            if (splitTo.from.isBefore(splitFrom.from) || splitTo.from.isAfter(splitFrom.to)) {
                throw IllegalArgumentException("Split time ${splitTo.from} is outside worklog range")
            }

            updateWork(splitFrom)
            val newId = addWork(splitTo)

            // Verify both operations succeeded
            val updatedFrom = findWorkById(splitFrom.workId)
            val newWorklog = findWorkById(newId)

            if (updatedFrom == null || newWorklog == null) {
                throw IllegalStateException("Split operation failed - one or both worklogs not found after operation")
            }

            return newId
        } catch (e: Exception) {
            // Log the error for debugging
            Timber.e(e, "Failed to split worklog from ${splitFrom.workId} to new worklog")
            throw e
        }
    }
}
