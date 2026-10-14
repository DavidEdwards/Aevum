package dae.aevum.database.daos

import androidx.paging.PagingSource
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

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addWorksIgnore(entity: List<WorklogEntity>)

    @Query("SELECT * FROM worklogs WHERE workId = :workId")
    suspend fun findWorkById(workId: Long): WorklogEntity?

    @Query(
        "SELECT * FROM worklogs WHERE userId = :userId AND jiraWorklogId = :jiraWorklogId LIMIT 1",
    )
    suspend fun findByJiraWorklogId(
        userId: Int,
        jiraWorklogId: Long,
    ): WorklogEntity?

    @Query("DELETE FROM worklogs WHERE userId = :userId")
    suspend fun deleteWorklogsForUser(userId: Int): Int

    @Query(
        "SELECT jiraWorklogId FROM worklogs WHERE userId = :userId AND pending = 0 " +
            "AND jiraWorklogId IS NOT NULL " +
            "AND `from` < :rangeEnd AND `to` > :rangeStart",
    )
    suspend fun getSyncedJiraIdsInRange(
        userId: Int,
        rangeStart: Instant,
        rangeEnd: Instant,
    ): List<Long>

    @Query(
        "DELETE FROM worklogs WHERE userId = :userId AND pending = 0 " +
            "AND jiraWorklogId IS NOT NULL " +
            "AND jiraWorklogId NOT IN (:keepIds) " +
            "AND `from` < :rangeEnd AND `to` > :rangeStart",
    )
    suspend fun pruneSyncedMissingFromRange(
        userId: Int,
        keepIds: List<Long>,
        rangeStart: Instant,
        rangeEnd: Instant,
    ): Int

    /**
     * Upsert remote (non-pending) worklogs by jiraWorklogId without clobbering local pending rows.
     */
    @Transaction
    suspend fun upsertRemoteWorks(entities: List<WorklogEntity>) {
        entities.forEach { entity ->
            val jiraId = entity.jiraWorklogId ?: return@forEach
            val existing = findByJiraWorklogId(entity.userId, jiraId)
            if (existing != null) {
                if (existing.pending) {
                    // Never overwrite a pending local row via remote id collision
                    return@forEach
                }
                updateWork(
                    entity.copy(
                        workId = existing.workId,
                        pending = false,
                        active = false,
                    ),
                )
            } else {
                addWork(entity.copy(workId = 0L))
            }
        }
    }

    @Query(
        "SELECT * FROM worklogs WHERE issueId = :issueId AND userId = :userId " +
            "ORDER BY `from` DESC LIMIT 500",
    )
    fun flowWorkByIssueId(issueId: String, userId: Int): Flow<List<WorklogEntity>>

    @Query(
        "SELECT * FROM worklogs WHERE issueId = :issueId AND userId = :userId " +
            "ORDER BY `from` DESC",
    )
    fun pagingWorkByIssueId(issueId: String, userId: Int): PagingSource<Int, WorklogEntity>

    @Query("SELECT * FROM worklogs WHERE active = 1 LIMIT 1")
    suspend fun getActiveWorklog(): WorklogEntity?

    @Query("SELECT * FROM worklogs WHERE active = 1 AND userId = :userId LIMIT 1")
    suspend fun getActiveWorklogForUser(userId: Int): WorklogEntity?

    /** Most recently completed (non-active) worklog for choosing a “start last” target. */
    @Query(
        "SELECT * FROM worklogs WHERE userId = :userId AND active = 0 " +
            "ORDER BY `to` DESC LIMIT 1",
    )
    suspend fun getLastCompletedWorklogForUser(userId: Int): WorklogEntity?

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

    @Query("SELECT * FROM worklogs WHERE userId = :userId AND pending = 1 ORDER BY `from` DESC")
    fun pagingPendingWorklogs(userId: Int): PagingSource<Int, WorklogEntity>

    @Query(
        "SELECT * FROM worklogs WHERE userId = :userId " +
            "AND `from` < :dayEnd AND `to` > :dayStart " +
            "ORDER BY `from` DESC",
    )
    fun flowWorklogsToday(
        userId: Int,
        dayStart: Instant,
        dayEnd: Instant,
    ): Flow<List<WorklogEntity>>

    @Query(
        "SELECT * FROM worklogs WHERE userId = :userId AND jiraUserAccountId = :jiraUserAccountId " +
            "AND pending = 0 AND active = 0 " +
            "AND `from` < :dayEnd AND `to` > :dayStart " +
            "ORDER BY `from` DESC LIMIT 500",
    )
    fun getWorklogsTodayFromMe(
        userId: Int,
        dayStart: Instant,
        dayEnd: Instant,
        jiraUserAccountId: String,
    ): Flow<List<WorklogEntity>>

    /**
     * Returns all worklogs authored by the current Jira user that overlap with the provided range.
     * Overlap condition uses: from < end AND to > start
     */
    @Query(
        "SELECT * FROM worklogs " +
            "WHERE userId = :userId " +
            "AND jiraUserAccountId = :jiraUserAccountId " +
            "AND `from` < :end " +
            "AND `to` > :start " +
            "ORDER BY `from` ASC",
    )
    fun getWorklogsFromMeBetween(
        userId: Int,
        start: Instant,
        end: Instant,
        jiraUserAccountId: String,
    ): Flow<List<WorklogEntity>>

    @Query(
        "SELECT worklogs.*, issues.title AS issueTitle FROM worklogs, issues " +
            "WHERE worklogs.issueId = issues.id " +
            "AND worklogs.userId = issues.userId " +
            "AND worklogs.userId = :userId",
    )
    fun getWorklogsAndIssues(userId: Int): Flow<List<WorklogIssue>>

    @Query(
        "SELECT worklogs.*, issues.title AS issueTitle FROM worklogs, issues " +
            "WHERE worklogs.issueId = issues.id " +
            "AND worklogs.userId = issues.userId " +
            "AND worklogs.userId = :userId " +
            "AND worklogs.jiraUserAccountId = :jiraUserAccountId " +
            "AND worklogs.pending = 0 " +
            "AND worklogs.active = 0 " +
            "AND worklogs.`from` < :dayEnd AND worklogs.`to` > :dayStart " +
            "ORDER BY worklogs.`from` DESC",
    )
    fun getWorklogIssuesTodayFromMe(
        userId: Int,
        dayStart: Instant,
        dayEnd: Instant,
        jiraUserAccountId: String,
    ): Flow<List<WorklogIssue>>

    @Query(
        "SELECT worklogs.*, issues.title AS issueTitle FROM worklogs, issues " +
            "WHERE worklogs.issueId = issues.id " +
            "AND worklogs.userId = issues.userId " +
            "AND worklogs.userId = :userId " +
            "AND worklogs.jiraUserAccountId = :jiraUserAccountId " +
            "AND worklogs.`from` < :rangeEnd AND worklogs.`to` > :rangeStart " +
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
