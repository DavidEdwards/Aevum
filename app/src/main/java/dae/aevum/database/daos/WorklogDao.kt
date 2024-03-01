package dae.aevum.database.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dae.aevum.database.entities.WorklogEntity
import dae.aevum.database.entities.composites.WorklogIssue
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface WorklogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addWork(entity: WorklogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addWorks(entity: List<WorklogEntity>)

    @Query("SELECT * FROM worklogs WHERE workId = :workId")
    suspend fun findWorkById(workId: Long): WorklogEntity

    @Query("SELECT * FROM worklogs WHERE issueId = :issueId ORDER BY `from` DESC LIMIT 50")
    fun flowWorkByIssueId(issueId: String): Flow<List<WorklogEntity>>

    @Query("SELECT * FROM worklogs WHERE active = 1 LIMIT 1")
    suspend fun getActiveWorklog(): WorklogEntity?

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

    @Query("SELECT * FROM worklogs WHERE userId = :userId AND ((`from` BETWEEN :dayStart AND :dayEnd) OR (`to` BETWEEN :dayStart AND :dayEnd)) ORDER BY `from` DESC")
    fun flowWorklogsToday(
        userId: Int,
        dayStart: Instant,
        dayEnd: Instant
    ): Flow<List<WorklogEntity>>

    @Query("SELECT * FROM worklogs WHERE userId = :userId AND jiraUserAccountId = :jiraUserAccountId AND ((`from` BETWEEN :dayStart AND :dayEnd) OR (`to` BETWEEN :dayStart AND :dayEnd)) ORDER BY `from` DESC")
    fun getWorklogsTodayFromMe(
        userId: Int,
        dayStart: Instant,
        dayEnd: Instant,
        jiraUserAccountId: String
    ): Flow<List<WorklogEntity>>


    @Query("SELECT worklogs.*, issues.title AS issueTitle FROM worklogs, issues WHERE worklogs.issueId = issues.id")
    fun getWorklogsAndIssues(): Flow<List<WorklogIssue>>

    @Query("SELECT worklogs.*, issues.title AS issueTitle FROM worklogs, issues WHERE worklogs.issueId = issues.id AND worklogs.userId = :userId AND worklogs.jiraUserAccountId = :jiraUserAccountId AND ((worklogs.`from` BETWEEN :dayStart AND :dayEnd) OR (worklogs.`to` BETWEEN :dayStart AND :dayEnd)) ORDER BY worklogs.`from` DESC")
    fun getWorklogIssuesTodayFromMe(
        userId: Int,
        dayStart: Instant,
        dayEnd: Instant,
        jiraUserAccountId: String
    ): Flow<List<WorklogIssue>>
}