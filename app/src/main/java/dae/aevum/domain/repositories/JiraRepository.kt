package dae.aevum.domain.repositories

import androidx.room.withTransaction
import dae.aevum.App
import dae.aevum.database.AppDatabase
import dae.aevum.database.entities.ActiveIssueEntity
import dae.aevum.database.entities.IssueEntity
import dae.aevum.database.entities.WorklogEntity
import dae.aevum.network.JiraService
import dae.aevum.network.models.CommentNodeRetro
import dae.aevum.network.models.SearchRequestRetro
import dae.aevum.network.models.WorklogAddRetro
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
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import javax.inject.Singleton
import kotlin.math.max

interface JiraRepository {
    suspend fun getIssues(): List<IssueEntity>
    fun flowIssues(): Flow<List<IssueEntity>>
    suspend fun refreshIssuesfromJira()
    suspend fun refreshWorklogsFor(issueId: IssueId)
    fun flowIssue(issueId: IssueId): Flow<IssueEntity>
    fun flowWorklog(worklogId: Long): Flow<WorklogEntity?>
    fun worklogsForIssue(issueId: IssueId): Flow<List<WorklogEntity>>

    suspend fun startLogFor(issueId: IssueId)
    fun flowActiveIssue(): Flow<ActiveIssueEntity?>
    suspend fun stopLogging(summary: String)
    suspend fun deleteWorklog(worklogId: Long)
    fun getPendingWorklogs(): Flow<List<WorklogEntity>>
    fun getWorklogsToday(): Flow<List<WorklogEntity>>
    suspend fun postPendingWorklogs(): List<WorklogEntity>
    suspend fun updateWorklog(worklogId: Long, from: Instant, to: Instant, summary: String)
    suspend fun toggleIssuePin(issueId: IssueId)
}

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class JiraRepositoryImpl(
    private val jira: JiraService,
    private val userRepository: UserRepository,
    private val database: AppDatabase
) : JiraRepository {
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

//            val formattedAfter = App.universalDateFormatter.format(
//                Instant.now().minus(30, ChronoUnit.DAYS)
//            )

            yield()

            val searches = listOf(
                "issuekey in issueHistory() ORDER BY lastViewed DESC",
                "worklogAuthor=currentUser() ORDER BY lastViewed DESC",
                "assignee=currentUser() ORDER BY lastViewed DESC"
            )

            val asyncSearches = searches.map { jql ->
                async(Dispatchers.IO) {
                    jira.searchIssues(SearchRequestRetro(jql)).execute()
                }
            }
            val asyncResults = asyncSearches.awaitAll()
            yield()

            val entityList = mutableListOf<IssueEntity>()

            asyncResults.forEach { response ->
                if (response.isSuccessful) {
                    Timber.v("Successfully ran search")
                    val body = response.body()
                    if (body != null) {
                        Timber.v("Search contains ${body.issues} issues.")
                        val issueEntities = body.issues.mapIndexed { index, issueRetro ->
                            IssueEntity(
                                id = IssueId(issueRetro.key),
                                userId = user.id,
                                title = issueRetro.fields.summary,
                                pinned = false,
                                sort = index
                            )
                        }

                        entityList.addAll(issueEntities)
                    } else {
                        Timber.e("No body")
                    }
                } else {
                    Timber.e(response.message())
                }
            }

            val dao = database.issueDao()
            database.withTransaction {
                dao.removeSorting(user.id)
                dao.addIssuesSafely(entityList)

                entityList.forEach {
                    dao.updateSortingFor(user.id, it.id.value, it.sort)
                }
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    override suspend fun refreshWorklogsFor(issueId: IssueId) {
        try {
            val user = userRepository.getActiveUser()
                ?: return

            yield()
            val response = withContext(Dispatchers.IO) {
                val call = jira.listWorklogsFor(issueId.value)
                call.execute()
            }

            yield()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    val entities = body.worklogs.map {
                        val parsedTime =
                            LocalDateTime.parse(it.started, App.universalDateTimeZoneFormatter)
                                .toInstant(ZoneOffset.UTC)
                        WorklogEntity(
                            workId = it.id.toLong(),
                            issueId = issueId,
                            userId = user.id,
                            from = parsedTime,
                            to = parsedTime.plusSeconds(it.timeSpentSeconds),
                            author = it.author.displayName,
                            summary = it.comment?.generateUnifiedString() ?: "No comment",
                            pending = false,
                            active = false
                        )
                    }
                    database.withTransaction {
                        database.worklogDao().addWorks(entities)
                    }
                } else {
                    Timber.e("No body")
                }
            } else {
                Timber.e(response.message())
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    override fun flowIssue(issueId: IssueId): Flow<IssueEntity> {
        return userRepository.flowActiveUser().flatMapLatest { user ->
            if (user == null) return@flatMapLatest emptyFlow()
            database.issueDao().flowIssueById(user.id, issueId.value)
        }
    }

    override fun flowWorklog(worklogId: Long): Flow<WorklogEntity?> {
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
        return database.worklogDao().addWork(
            WorklogEntity(
                workId = 0L,
                issueId = issueId,
                userId = user.id,
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
        val worklog = database.worklogDao().getActiveWorklog()

        val worklogCopy = worklog?.copy(
            active = false,
            to = Instant.now(),
            summary = summary
        ) ?: return

        database.withTransaction {
            database.worklogDao().updateWork(worklogCopy)
        }
    }

    override suspend fun deleteWorklog(worklogId: Long) {
        database.withTransaction {
            database.worklogDao().deleteWorkById(worklogId)
        }
    }

    override fun getPendingWorklogs(): Flow<List<WorklogEntity>> {
        return database.worklogDao().flowPendingWorklogs()
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

    override suspend fun postPendingWorklogs(): List<WorklogEntity> = supervisorScope {
        val worklogs = getPendingWorklogs().firstOrNull()
            ?.filter { !it.active }
            ?: emptyList()

        yield()
        val updatedWorklogs = worklogs.map { worklog ->
            async(Dispatchers.IO) {
                val secondsDuration =
                    (Duration.between(worklog.from, worklog.to).toMillis() / 1000).toInt()

                yield()
                val response = jira.addWorklog(
                    worklog.issueId.value,
                    WorklogAddRetro(
                        CommentNodeRetro.generateFromString(worklog.summary),
                        App.universalDateTimeZoneFormatter.withZone(ZoneId.of("UTC"))
                            .format(worklog.from),
                        max(secondsDuration, 60)
                    )
                ).execute()

                if (response.isSuccessful) {
                    Timber.v("Posted ${worklog.workId}(${worklog.issueId}) to Jira")
                    worklog.copy(pending = false)
                } else {
                    Timber.e("Could not post ${worklog.workId}(${worklog.issueId}) to Jira")
                    worklog
                }
            }
        }.awaitAll()

        database.worklogDao().updateWorklogs(updatedWorklogs)

        updatedWorklogs
    }

    override suspend fun updateWorklog(
        worklogId: Long,
        from: Instant,
        to: Instant,
        summary: String
    ) {
        val entity = database.worklogDao().findWorkById(worklogId)
        val newEntity = entity.copy(
            from = from,
            to = to,
            summary = summary
        )
        database.worklogDao().updateWork(newEntity)
    }

    override suspend fun toggleIssuePin(issueId: IssueId) {
        val user = userRepository.getActiveUser()
            ?: return

        database.issueDao().toggleIssuePin(user.id, issueId.value)
    }
}