package dae.aevum.network

import dae.aevum.database.entities.IssueEntity
import dae.aevum.database.entities.UserEntity
import dae.aevum.database.entities.WorklogEntity
import dae.aevum.network.models.CommentNodeRetro
import dae.aevum.network.models.UserRetro
import dae.aevum.network.models.WorklogAddRetro
import dae.aevum.utils.Formatters
import dae.aevum.utils.IssueId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import timber.log.Timber
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.ceil
import kotlin.math.max

class Jira(
    private val jiraService: JiraService,
) {
    suspend fun getJiraAccount(email: String): UserRetro {
        val body =
            withContext(Dispatchers.IO) {
                jiraService.getUser(email).execute().body()
            } ?: throw IllegalArgumentException("Could not get Jira account id for $email")

        return body.users.firstOrNull()
            ?: throw IllegalArgumentException("Could not get Jira account id for $email")
    }

    suspend fun searchIssues(
        userId: Int,
        searches: List<String>,
    ) = withContext(Dispatchers.IO) {
        val asyncSearches =
            searches.map { jql ->
                async(Dispatchers.IO) {
                    jiraService
                        .searchIssues(
                            jql = jql,
                            fields = "id,summary,key",
                            maxResults = 1000,
                            startAt = 0,
                        ).execute()
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
                    val issueEntities =
                        body.issues.mapIndexed { index, issueRetro ->
                            IssueEntity(
                                id = IssueId(issueRetro.key),
                                userId = userId,
                                title = issueRetro.fields.summary,
                                pinned = false,
                                sort = index,
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

        entityList
    }

    /**
     * Uses Jira's issue picker (same API as the in-product issue chooser).
     * Matches issue keys by number suffix (e.g. "5712" → PROJ-5712) better than text JQL.
     */
    suspend fun pickIssues(
        userId: Int,
        query: String,
    ): List<IssueEntity> =
        withContext(Dispatchers.IO) {
            try {
                val response =
                    jiraService
                        .pickIssues(
                            query = query,
                            // Broad bound so "Current Search" can match keys outside history.
                            currentJql = "updated >= -365d ORDER BY updated DESC",
                        ).execute()
                if (!response.isSuccessful) {
                    Timber.e("Issue picker failed: ${response.message()}")
                    return@withContext emptyList()
                }
                val sections = response.body()?.sections.orEmpty()
                sections
                    .flatMap { it.issues }
                    .distinctBy { it.key }
                    .mapIndexed { index, issue ->
                        IssueEntity(
                            id = IssueId(issue.key),
                            userId = userId,
                            title = issue.summaryText ?: issue.summary.orEmpty(),
                            pinned = false,
                            sort = index,
                        )
                    }
            } catch (e: Exception) {
                Timber.e(e, "Issue picker exception for query=$query")
                emptyList()
            }
        }

    suspend fun loadIssue(
        userId: Int,
        issueKey: String,
    ): IssueEntity? {
        try {
            val response =
                withContext(Dispatchers.IO) {
                    jiraService
                        .searchIssues(
                            jql = "key = $issueKey",
                            fields = "summary,key",
                            maxResults = 1,
                            startAt = 0,
                        ).execute()
                }

            yield()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.issues.isNotEmpty()) {
                    val issueRetro = body.issues.first()
                    return IssueEntity(
                        id = IssueId(issueRetro.key),
                        userId = userId,
                        title = issueRetro.fields.summary,
                        pinned = false,
                        sort = 0,
                    )
                } else {
                    Timber.e("No body or no issues found for key: $issueKey")
                }
            } else {
                Timber.e("Failed to load issue $issueKey: ${response.message()}")
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception loading issue $issueKey")
        }
        return null
    }

    suspend fun refreshWorklogsFor(
        issueId: IssueId,
        user: UserEntity,
    ): List<WorklogEntity> {
        try {
            val startedAfter = System.currentTimeMillis() - JiraService.ONE_MONTH_IN_MILLIS
            val remotes = listAllWorklogsFor(issueId.value, startedAfter)
            return remotes
                .filter { it.author.accountId == user.jiraUserAccountId }
                .map { it.toEntity(issueId, user) }
        } catch (e: Exception) {
            Timber.e(e)
            throw e
        }
    }

    private suspend fun listAllWorklogsFor(
        issueKey: String,
        startedAfter: Long,
    ): List<dae.aevum.network.models.WorklogRetro> {
        val all = mutableListOf<dae.aevum.network.models.WorklogRetro>()
        var startAt = 0
        val pageSize = 100
        while (true) {
            val response =
                withContext(Dispatchers.IO) {
                    jiraService
                        .listWorklogsFor(
                            issueId = issueKey,
                            startedAfter = startedAfter,
                            startAt = startAt,
                            maxResults = pageSize,
                        ).execute()
                }
            yield()
            if (!response.isSuccessful) {
                Timber.e(response.message())
                break
            }
            val body = response.body() ?: break
            all.addAll(body.worklogs)
            startAt += body.worklogs.size
            if (startAt >= body.total || body.worklogs.isEmpty()) break
        }
        return all
    }

    private fun dae.aevum.network.models.WorklogRetro.toEntity(
        issueId: IssueId,
        user: UserEntity,
    ): WorklogEntity {
        val parsedTime =
            ZonedDateTime
                .parse(started, Formatters.universalDateTimeZoneFormatter)
                .toInstant()
        return WorklogEntity(
            workId = 0L,
            jiraWorklogId = id.toLongOrNull(),
            issueId = issueId,
            userId = user.id,
            jiraUserAccountId = author.accountId,
            from = parsedTime,
            to = parsedTime.plusSeconds(timeSpentSeconds),
            author = author.displayName,
            summary = comment?.generateUnifiedString() ?: "No comment",
            pending = false,
            active = false,
        )
    }

    suspend fun refreshWorklogsToday(user: UserEntity): List<WorklogEntity> {
        try {
            // Get all issues for the user
            val issues =
                withContext(Dispatchers.IO) {
                    val call =
                        jiraService.searchIssues(
                            jql = "worklogAuthor = currentUser() AND worklogDate >= startOfDay() AND worklogDate <= endOfDay()",
                            fields = "key",
                            maxResults = 1000,
                            startAt = 0,
                        )
                    call.execute()
                }

            yield()
            if (issues.isSuccessful) {
                val body = issues.body()
                if (body != null) {
                    val issueKeys = body.issues.map { it.key }

                    // Fetch worklogs for each issue
                    val allWorklogs = mutableListOf<WorklogEntity>()

                    issueKeys.forEach { issueKey ->
                        val worklogs = refreshWorklogsFor(IssueId(issueKey), user)
                        allWorklogs.addAll(worklogs)
                    }

                    return allWorklogs
                } else {
                    Timber.e("No body for issues")
                    return emptyList()
                }
            } else {
                Timber.e(issues.message())
                error("Jira search failed: ${issues.code()} ${issues.message()}")
            }
        } catch (e: Exception) {
            Timber.e(e)
            throw e
        }
    }

    fun updateWorklog(worklog: WorklogEntity): WorklogEntity {
        val secondsDuration =
            ceil(
                Duration.between(worklog.from, worklog.to).toMillis() / 1000.0 / 60.0,
            ).toInt() * 60

        val response =
            jiraService
                .addWorklog(
                    worklog.issueId.value,
                    WorklogAddRetro(
                        CommentNodeRetro.generateFromString(worklog.summary),
                        Formatters.universalDateTimeZoneFormatter
                            .withZone(ZoneId.of("UTC"))
                            .format(worklog.from),
                        max(secondsDuration, 60),
                    ),
                ).execute()

        return if (response.isSuccessful) {
            Timber.v("Posted ${worklog.workId}(${worklog.issueId}) to Jira")
            worklog.copy(pending = false)
        } else {
            Timber.e("Could not post ${worklog.workId}(${worklog.issueId}) to Jira")
            worklog
        }
    }

    suspend fun testUser(
        instanceUrl: String,
        authHeader: String,
    ): Boolean =
        try {
            withContext(Dispatchers.IO) {
                val base = instanceUrl.trimEnd('/')
                val url = "$base/rest/api/3/search/jql?jql=worklogAuthor%20%3D%20currentUser()&maxResults=1"
                val request =
                    okhttp3.Request
                        .Builder()
                        .url(url)
                        .header("Authorization", authHeader)
                        .header("Accept", "application/json")
                        .get()
                        .build()
                val client = okhttp3.OkHttpClient()
                client.newCall(request).execute().use { response ->
                    response.isSuccessful
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "testUser failed for $instanceUrl")
            false
        }

    suspend fun refreshWorklogsInRange(
        user: UserEntity,
        start: Instant,
        end: Instant,
    ): List<WorklogEntity> {
        try {
            val zone = ZoneId.systemDefault()
            val startDate = start.atZone(zone).toLocalDate()
            // end is exclusive (start of tomorrow); JQL worklogDate is inclusive
            val endDate = end.atZone(zone).toLocalDate().minusDays(1)

            val jql =
                "worklogAuthor = currentUser() AND worklogDate >= $startDate AND worklogDate <= $endDate"
            val issueKeys = searchAllIssueKeys(jql)
            val startedAfter = start.toEpochMilli()

            val allWorklogs = mutableListOf<WorklogEntity>()
            issueKeys.forEach { issueKey ->
                val remotes = listAllWorklogsFor(issueKey, startedAfter)
                val filtered =
                    remotes
                        .filter { it.author.accountId == user.jiraUserAccountId }
                        .map { it.toEntity(IssueId(issueKey), user) }
                        .filter { entity ->
                            entity.from.isBefore(end) && entity.to.isAfter(start)
                        }
                allWorklogs.addAll(filtered)
            }

            return allWorklogs
        } catch (e: Exception) {
            Timber.e(e)
            throw e
        }
    }

    private suspend fun searchAllIssueKeys(jql: String): List<String> {
        val keys = mutableListOf<String>()
        var startAt = 0
        val pageSize = 100
        while (true) {
            val response =
                withContext(Dispatchers.IO) {
                    jiraService
                        .searchIssues(
                            jql = jql,
                            fields = "key",
                            maxResults = pageSize,
                            startAt = startAt,
                        ).execute()
                }
            yield()
            if (!response.isSuccessful) {
                Timber.e(response.message())
                break
            }
            val body = response.body() ?: break
            keys.addAll(body.issues.map { it.key })
            startAt += body.issues.size
            if (startAt >= body.total || body.issues.isEmpty()) break
        }
        return keys
    }
}
