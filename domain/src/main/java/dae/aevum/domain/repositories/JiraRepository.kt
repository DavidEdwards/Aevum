package dae.aevum.domain.repositories

import android.util.Base64
import dae.aevum.database.AppDatabase
import dae.aevum.database.entities.ActiveIssueEntity
import dae.aevum.database.entities.IssueEntity
import dae.aevum.database.entities.WorklogEntity
import dae.aevum.database.entities.composites.WorklogIssue
import dae.aevum.network.Jira
import dae.aevum.utils.IssueId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import timber.log.Timber
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Singleton
import android.content.Intent
import dae.aevum.utils.Intents

interface JiraRepository {
    suspend fun getJiraAccountId(email: String): String
    suspend fun getIssues(): List<IssueEntity>
    fun flowIssues(): Flow<List<IssueEntity>>
    suspend fun refreshIssuesfromJira()
    suspend fun refreshWorklogsFor(issueId: IssueId)
    suspend fun refreshWorklogsToday()
    fun flowIssue(issueId: IssueId): Flow<IssueEntity>
    fun flowWorklog(worklogId: Long): Flow<WorklogIssue?>
    fun worklogsForIssue(issueId: IssueId): Flow<List<WorklogEntity>>

    suspend fun startLogFor(issueId: IssueId)
    fun flowActiveIssue(): Flow<ActiveIssueEntity?>
    suspend fun stopLogging(summary: String)
    suspend fun deleteWorklog(worklogId: Long)
    fun getPendingWorklogs(): Flow<List<WorklogEntity>>
    fun getWorklogsToday(): Flow<List<WorklogEntity>>
    fun getWorklogsTodayFromMe(): Flow<List<WorklogIssue>>
    fun getWorklogIssuesInLastTwoWeeks(): Flow<List<WorklogIssue>>
    suspend fun postPendingWorklogs(): List<WorklogEntity>
    suspend fun updateWorklog(worklogId: Long, from: Instant, to: Instant, summary: String)
    suspend fun incrementWorklogTime(worklogId: Long, amount: Long)
    suspend fun decrementWorklogTime(worklogId: Long, amount: Long)
    suspend fun moveWorklogIssue(worklogId: Long, issueId: IssueId)
    suspend fun splitWorklog(
        worklogId: Long,
        newIssueId: IssueId,
        splitAt: Instant,
        withNewId: suspend (Long) -> Unit = {}
    ): Boolean

    suspend fun toggleIssuePin(issueId: IssueId)
    suspend fun testUser(
        instanceUrl: String,
        user: String,
        token: String
    ): Boolean

    suspend fun refreshWorklogsForLastTwoWeeks()
}

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class JiraRepositoryImpl(
    private val jira: Jira,
    private val userRepository: UserRepository,
    private val database: AppDatabase
) : JiraRepository {

    override suspend fun getJiraAccountId(email: String): String {
        return jira.getJiraAccount(email).accountId
    }

    override suspend fun getIssues(): List<IssueEntity> {
        val user = userRepository.getActiveUser()
            ?: return emptyList()

        return database.issueDao().getAllIssues(user.id)
    }

    override fun flowIssues() = userRepository.flowActiveUser().flatMapLatest { user ->
        if (user == null) return@flatMapLatest emptyFlow()
        database.issueDao().flowAllIssues(user.id)
    }

    override suspend fun refreshIssuesfromJira() = supervisorScope {
        return@supervisorScope try {
            val user = userRepository.getActiveUser()
                ?: return@supervisorScope

            yield()

            val searches = listOf(
                "issuekey in issueHistory() ORDER BY lastViewed DESC",
                "worklogAuthor=currentUser() ORDER BY lastViewed DESC",
                "assignee=currentUser() ORDER BY lastViewed DESC"
            )

            val entityList = jira.searchIssues(user.id, searches)

            database.issueDao().refreshWith(user.id, entityList)
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    override suspend fun refreshWorklogsFor(issueId: IssueId) {
        try {
            val user = userRepository.getActiveUser()
                ?: return

            // Ensure the issue exists in the database
            ensureIssueExists(user.id, issueId)

            yield()
            val entities = withContext(Dispatchers.IO) {
                jira.refreshWorklogsFor(
                    issueId,
                    user
                )
            }

            yield()
            if (entities.isNotEmpty()) {
                database.worklogDao().addWorks(entities)
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    override suspend fun refreshWorklogsToday() {
        try {
            val user = userRepository.getActiveUser()
                ?: return

            yield()
            val entities = withContext(Dispatchers.IO) {
                jira.refreshWorklogsToday(user)
            }

            yield()
            if (entities.isNotEmpty()) {
                // Ensure all issues referenced in worklogs exist in the database
                val issueIds = entities.map { it.issueId }.distinct()
                issueIds.forEach { issueId ->
                    ensureIssueExists(user.id, issueId)
                }
                
                database.worklogDao().addWorks(entities)
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    private suspend fun ensureIssueExists(userId: Int, issueId: IssueId) {
        try {
            val exists = database.issueDao().issueExists(userId, issueId.value)
            if (exists == 0) {
                Timber.v("Issue ${issueId.value} not found in database, loading from Jira...")
                val issue = jira.loadIssue(userId, issueId.value)
                if (issue != null) {
                    database.issueDao().addIssue(issue)
                    Timber.v("Successfully loaded issue ${issueId.value} from Jira")
                } else {
                    Timber.w("Failed to load issue ${issueId.value} from Jira")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error ensuring issue ${issueId.value} exists")
        }
    }

    override fun flowIssue(issueId: IssueId): Flow<IssueEntity> {
        return userRepository.flowActiveUser().flatMapLatest { user ->
            if (user == null) return@flatMapLatest emptyFlow()
            database.issueDao().flowIssueById(user.id, issueId.value)
        }
    }

    override fun flowWorklog(worklogId: Long): Flow<WorklogIssue?> {
        return userRepository.flowActiveUser().flatMapLatest { user ->
            if (user == null) return@flatMapLatest emptyFlow()
            database.issueDao().flowWorklogById(user.id, worklogId)
        }
    }

    override fun worklogsForIssue(issueId: IssueId): Flow<List<WorklogEntity>> {
        return database.worklogDao().flowWorkByIssueId(issueId.value)
    }

    override suspend fun startLogFor(
        issueId: IssueId
    ) {
        val user = userRepository.getActiveUser()
            ?: return

        val now = Instant.now()
        database.worklogDao().addWork(
            WorklogEntity(
                workId = 0L,
                issueId = issueId,
                userId = user.id,
                jiraUserAccountId = user.jiraUserAccountId,
                from = now,
                to = now,
                author = "Pending",
                summary = "",
                pending = true,
                active = true
            )
        )
    }

    override fun flowActiveIssue(): Flow<ActiveIssueEntity?> {
        return userRepository.flowActiveUser().flatMapLatest { user ->
            if (user == null) return@flatMapLatest emptyFlow()
            database.issueDao().flowActiveIssue(user.id)
        }
    }

    override suspend fun stopLogging(summary: String) {
        val user = userRepository.getActiveUser() ?: return
        val allWorks = database.worklogDao().getAllWorks()
        val activeForUser = allWorks.filter { it.active && it.userId == user.id }
        if (activeForUser.isEmpty()) return

        val now = Instant.now()
        val mostRecent = activeForUser.maxByOrNull { it.from }

        activeForUser.forEach { entity ->
            val updated = entity.copy(
                active = false,
                to = now,
                summary = if (entity.workId == mostRecent?.workId) summary else entity.summary
            )
            database.worklogDao().updateWork(updated)
        }
    }

    override suspend fun deleteWorklog(worklogId: Long) {
        database.worklogDao().deleteWorkById(worklogId)
    }

    override fun getPendingWorklogs(): Flow<List<WorklogEntity>> {
        return userRepository.flowActiveUser().flatMapLatest { user ->
            user ?: return@flatMapLatest emptyFlow()

            return@flatMapLatest database.worklogDao().flowPendingWorklogs(user.id)
        }
    }

    override fun getWorklogsToday(): Flow<List<WorklogEntity>> {
        return userRepository.flowActiveUser().flatMapLatest { user ->
            user ?: return@flatMapLatest emptyFlow()

            val dayStart = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC)
            val dayEnd = LocalDate.now().atStartOfDay().plusHours(24).toInstant(ZoneOffset.UTC)

            return@flatMapLatest database.worklogDao().flowWorklogsToday(
                userId = user.id,
                dayStart = dayStart,
                dayEnd = dayEnd
            )
        }
    }

    override fun getWorklogsTodayFromMe(): Flow<List<WorklogIssue>> {
        return userRepository.flowActiveUser().flatMapLatest { user ->
            user ?: return@flatMapLatest emptyFlow()

            val dayStart = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC)
            val dayEnd = LocalDate.now().atStartOfDay().plusHours(24).toInstant(ZoneOffset.UTC)

            return@flatMapLatest database.worklogDao().getWorklogIssuesTodayFromMe(
                userId = user.id,
                dayStart = dayStart,
                dayEnd = dayEnd,
                jiraUserAccountId = user.jiraUserAccountId
            )
        }
    }

    override fun getWorklogIssuesInLastTwoWeeks(): Flow<List<WorklogIssue>> {
        return userRepository.flowActiveUser().flatMapLatest { user ->
            user ?: return@flatMapLatest emptyFlow()
            val end = LocalDate.now().atStartOfDay().plusHours(24).toInstant(ZoneOffset.UTC)
            val start = LocalDate.now().minusDays(13).atStartOfDay().toInstant(ZoneOffset.UTC)
            return@flatMapLatest database.worklogDao().getWorklogIssuesInRangeFromMe(
                userId = user.id,
                rangeStart = start,
                rangeEnd = end,
                jiraUserAccountId = user.jiraUserAccountId
            )
        }
    }

    override suspend fun postPendingWorklogs(): List<WorklogEntity> = supervisorScope {
        val worklogs = getPendingWorklogs().firstOrNull()
            ?.filter { !it.active }
            ?: emptyList()

        yield()
        val updatedWorklogs = worklogs.map { worklog ->
            async(Dispatchers.IO) {
                jira.updateWorklog(worklog)
            }
        }.awaitAll()

        database.worklogDao().updateWorklogs(updatedWorklogs)

        // Now we can remove non-pending and update from Jira
        val noLongerPendingWorklogs = updatedWorklogs.filter { it.pending.not() }
        noLongerPendingWorklogs.forEach {
            database.worklogDao().deleteWork(it)
        }

        val issuesWithNewUpdates = noLongerPendingWorklogs.map { it.issueId }.distinct()
        issuesWithNewUpdates.forEach {
            refreshWorklogsFor(it)
        }

        updatedWorklogs
    }

    override suspend fun refreshWorklogsForLastTwoWeeks() {
        try {
            val user = userRepository.getActiveUser() ?: return

            val end = LocalDate.now().atStartOfDay().plusHours(24).toInstant(ZoneOffset.UTC)
            val start = LocalDate.now().minusDays(13).atStartOfDay().toInstant(ZoneOffset.UTC)

            val entities = withContext(Dispatchers.IO) {
                jira.refreshWorklogsInRange(user, start, end)
            }

            if (entities.isNotEmpty()) {
                val issueIds = entities.map { it.issueId }.distinct()
                issueIds.forEach { issueId ->
                    ensureIssueExists(user.id, issueId)
                }
                database.worklogDao().addWorks(entities)
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    override suspend fun updateWorklog(
        worklogId: Long,
        from: Instant,
        to: Instant,
        summary: String
    ) {
        // Validate inputs
        if (worklogId <= 0) {
            throw IllegalArgumentException("Invalid worklog ID: $worklogId")
        }
        if (from.isAfter(to)) {
            throw IllegalArgumentException("Invalid time range: from $from to $to")
        }
        if (summary.isBlank()) {
            throw IllegalArgumentException("Summary cannot be blank")
        }

        val entity = database.worklogDao().findWorkById(worklogId)
            ?: throw IllegalArgumentException("Worklog not found with ID: $worklogId")

        val newEntity = entity.copy(
            from = from,
            to = to,
            summary = summary
        )
        database.worklogDao().updateWork(newEntity)
    }

    override suspend fun incrementWorklogTime(
        worklogId: Long,
        amount: Long
    ) {
        // Validate inputs
        if (worklogId <= 0) {
            throw IllegalArgumentException("Invalid worklog ID: $worklogId")
        }
        if (amount <= 0) {
            throw IllegalArgumentException("Amount must be positive: $amount")
        }

        val entity = database.worklogDao().findWorkById(worklogId)
            ?: throw IllegalArgumentException("Worklog not found with ID: $worklogId")

        var newInstant = entity.from.minusMillis(amount)
        if (newInstant.isAfter(Instant.now())) {
            newInstant = Instant.now().plusSeconds(1)
        }

        val newEntity = entity.copy(
            from = newInstant
        )
        database.worklogDao().updateWork(newEntity)
    }

    override suspend fun decrementWorklogTime(
        worklogId: Long,
        amount: Long
    ) {
        // Validate inputs
        if (worklogId <= 0) {
            throw IllegalArgumentException("Invalid worklog ID: $worklogId")
        }
        if (amount <= 0) {
            throw IllegalArgumentException("Amount must be positive: $amount")
        }

        val entity = database.worklogDao().findWorkById(worklogId)
            ?: throw IllegalArgumentException("Worklog not found with ID: $worklogId")

        var newInstant = entity.from.plusMillis(amount)
        if (newInstant.isAfter(Instant.now())) {
            newInstant = Instant.now().minusSeconds(1)
        }

        val newEntity = entity.copy(
            from = newInstant
        )
        database.worklogDao().updateWork(newEntity)
    }

    override suspend fun moveWorklogIssue(
        worklogId: Long,
        issueId: IssueId
    ) {
        val entity = database.worklogDao().findWorkById(worklogId)
            ?: throw IllegalArgumentException("Worklog not found with ID: $worklogId")

        val newEntity = entity.copy(
            issueId = issueId
        )
        database.worklogDao().updateWork(newEntity)
    }

    override suspend fun splitWorklog(
        worklogId: Long,
        newIssueId: IssueId,
        splitAt: Instant,
        withNewId: suspend (Long) -> Unit
    ): Boolean {
        try {
            val entity = database.worklogDao().findWorkById(worklogId)
                ?: throw IllegalArgumentException("Worklog not found with ID: $worklogId")

            if (!entity.pending || entity.active) {
                Timber.e("Tried to split either a worklog that is already sent or an active log. Only non-active, pending logs can be split.")
                return false
            }

            // Validate split time is within the worklog's time range
            if (splitAt.isBefore(entity.from) || splitAt.isAfter(entity.to)) {
                Timber.e("Split time $splitAt is outside worklog time range (${entity.from} to ${entity.to})")
                return false
            }

            val firstEntity = entity.copy(
                to = splitAt
            )
            val secondEntity = entity.copy(
                workId = 0,
                issueId = newIssueId,
                from = splitAt,
                summary = ""
            )

            val id = database.worklogDao().splitWork(firstEntity, secondEntity)

            // Verify the split operation succeeded by checking both worklogs exist
            val firstWorklog = database.worklogDao().findWorkById(worklogId)
            val secondWorklog = database.worklogDao().findWorkById(id)

            if (firstWorklog == null || secondWorklog == null) {
                Timber.e("Split operation failed - one or both worklogs not found after split")
                return false
            }

            withNewId(id)
            return true

        } catch (e: Exception) {
            Timber.e(e, "Exception during splitWorklog operation for worklogId: $worklogId")
            return false
        }
    }

    override suspend fun toggleIssuePin(issueId: IssueId) {
        val user = userRepository.getActiveUser()
            ?: return

        database.issueDao().toggleIssuePin(user.id, issueId.value)
    }

    override suspend fun testUser(
        instanceUrl: String,
        user: String,
        token: String
    ): Boolean {
        val authorizationToken = Base64.encodeToString(
            "${user}:${token}".toByteArray(),
            Base64.NO_WRAP
        )

        return jira.testUser(instanceUrl, "Basic $authorizationToken")
    }
}