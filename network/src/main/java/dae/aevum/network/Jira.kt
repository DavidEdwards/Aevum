package dae.aevum.network

import dae.aevum.database.entities.IssueEntity
import dae.aevum.database.entities.UserEntity
import dae.aevum.database.entities.WorklogEntity
import dae.aevum.network.models.CommentNodeRetro
import dae.aevum.network.models.SearchRequestRetro
import dae.aevum.network.models.SearchRetro
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
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.math.ceil
import kotlin.math.max

class Jira(private val jiraService: JiraService) {

    suspend fun getJiraAccount(email: String): UserRetro {
        val body = withContext(Dispatchers.IO) {
            jiraService.getUser(email).execute().body()
        } ?: throw IllegalArgumentException("Could not get Jira account id for $email")

        return body.users.firstOrNull()
            ?: throw IllegalArgumentException("Could not get Jira account id for $email")
    }

    suspend fun searchIssues(userId: Int, searches: List<String>) = withContext(Dispatchers.IO) {
        val asyncSearches = searches.map { jql ->
            async(Dispatchers.IO) {
                jiraService.searchIssues(SearchRequestRetro(jql)).execute()
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
                            userId = userId,
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

        entityList
    }

    suspend fun loadIssue(userId: Int, issueKey: String): IssueEntity? {
        try {
            val response = withContext(Dispatchers.IO) {
                jiraService.searchIssues(
                    SearchRequestRetro(
                        jql = "issuekey = $issueKey",
                        fields = listOf("summary")
                    )
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
                        sort = 0
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

    suspend fun refreshWorklogsFor(issueId: IssueId, user: UserEntity): List<WorklogEntity> {
        try {
            val response = withContext(Dispatchers.IO) {
                Timber.v("Fetching worklogs for issue: ${issueId.value}")
                val call = jiraService.listWorklogsFor(
                    issueId.value,
                    System.currentTimeMillis() - JiraService.ONE_WEEK_IN_MILLIS
                )
                call.execute()
            }

            yield()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    val entities = body.worklogs.map {
                        val parsedTime = LocalDateTime.parse(
                            it.started,
                            Formatters.universalDateTimeZoneFormatter
                        ).toInstant(ZoneOffset.UTC)

                        WorklogEntity(
                            workId = it.id.toLong(),
                            issueId = issueId,
                            userId = user.id,
                            jiraUserAccountId = it.author.accountId,
                            from = parsedTime,
                            to = parsedTime.plusSeconds(it.timeSpentSeconds),
                            author = it.author.displayName,
                            summary = it.comment?.generateUnifiedString() ?: "No comment",
                            pending = false,
                            active = false
                        )
                    }

                    return entities
                } else {
                    Timber.e("No body")
                }
            } else {
                Timber.e(response.message())
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
        return emptyList()
    }

    suspend fun refreshWorklogsToday(user: UserEntity): List<WorklogEntity> {
        try {
            // Get all issues for the user
            val issues = withContext(Dispatchers.IO) {
                val call = jiraService.searchIssues(
                    SearchRequestRetro(
                        jql = "worklogAuthor=currentUser() AND worklogDate >= startOfDay() AND worklogDate <= endOfDay()",
                        fields = listOf("key")
                    )
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
                }
            } else {
                Timber.e(issues.message())
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
        return emptyList()
    }

    fun updateWorklog(worklog: WorklogEntity): WorklogEntity {
        val secondsDuration =
            ceil(
                Duration.between(worklog.from, worklog.to).toMillis() / 1000.0 / 60.0
            ).toInt() * 60

        val response = jiraService.addWorklog(
            worklog.issueId.value,
            WorklogAddRetro(
                CommentNodeRetro.generateFromString(worklog.summary),
                Formatters.universalDateTimeZoneFormatter.withZone(ZoneId.of("UTC"))
                    .format(worklog.from),
                max(secondsDuration, 60)
            )
        ).execute()

        return if (response.isSuccessful) {
            Timber.v("Posted ${worklog.workId}(${worklog.issueId}) to Jira")
            worklog.copy(pending = false)
        } else {
            Timber.e("Could not post ${worklog.workId}(${worklog.issueId}) to Jira")
            worklog
        }
    }

    suspend fun testUser(instanceUrl: String, authHeader: String): Boolean {
        return try {
            val response = withContext(Dispatchers.IO) {
                jiraService.testUser(instanceUrl, authHeader).execute()
            }

            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }
}