package dae.aevum

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import dae.aevum.database.AppDatabase
import dae.aevum.domain.repositories.JiraRepository
import dae.aevum.domain.repositories.UserRepository
import dae.aevum.domain.repositories.WidgetRefresher
import dae.aevum.ui.composables.Router
import dae.aevum.ui.theme.AppTheme
import dae.aevum.utils.AssistantFeature
import dae.aevum.utils.Intents
import dae.aevum.utils.IssueId
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var jiraRepository: JiraRepository

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var database: AppDatabase

    @Inject
    lateinit var widgetRefresher: WidgetRefresher

    private var isReady = false
    private var pendingStopWorklogId by mutableStateOf<Long?>(null)
    private var pendingEditorWorklogId by mutableStateOf<Long?>(null)
    private var pendingSearchQuery by mutableStateOf<String?>(null)
    private var pendingOpenFeature by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        splashScreen.setKeepOnScreenCondition { !isReady }
        consumeDeepLinkExtras(intent)

        enableEdgeToEdge()
        setContent {
            AppTheme {
                Router(
                    onReady = { isReady = true },
                    pendingStopWorklogId = pendingStopWorklogId,
                    onStopDialogOpened = { pendingStopWorklogId = null },
                    pendingEditorWorklogId = pendingEditorWorklogId,
                    onEditorOpened = { pendingEditorWorklogId = null },
                    pendingSearchQuery = pendingSearchQuery,
                    onSearchQueryConsumed = { pendingSearchQuery = null },
                    pendingOpenFeature = pendingOpenFeature,
                    onOpenFeatureConsumed = { pendingOpenFeature = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeDeepLinkExtras(intent)
    }

    private fun consumeDeepLinkExtras(intent: Intent?) {
        if (intent == null) return

        val stopId = intent.getLongExtra(Intents.EXTRA_OPEN_STOP_WORKLOG_ID, -1L)
        if (stopId > 0) {
            pendingStopWorklogId = stopId
            intent.removeExtra(Intents.EXTRA_OPEN_STOP_WORKLOG_ID)
        }
        val editorId = intent.getLongExtra(Intents.EXTRA_OPEN_WORKLOG_EDITOR_ID, -1L)
        if (editorId > 0) {
            pendingEditorWorklogId = editorId
            intent.removeExtra(Intents.EXTRA_OPEN_WORKLOG_EDITOR_ID)
        }

        val searchQuery = intent.getStringExtra(Intents.EXTRA_ASSISTANT_SEARCH_QUERY)?.trim()
        if (!searchQuery.isNullOrEmpty()) {
            pendingSearchQuery = searchQuery
            pendingOpenFeature = Intents.FEATURE_ISSUES
            intent.removeExtra(Intents.EXTRA_ASSISTANT_SEARCH_QUERY)
        }

        val feature = AssistantFeature.normalize(intent.getStringExtra(Intents.EXTRA_ASSISTANT_OPEN_FEATURE))
        if (!feature.isNullOrEmpty()) {
            pendingOpenFeature = feature
            intent.removeExtra(Intents.EXTRA_ASSISTANT_OPEN_FEATURE)
        }

        val startIssueKey = intent.getStringExtra(Intents.EXTRA_ASSISTANT_START_ISSUE_KEY)?.trim()
        if (!startIssueKey.isNullOrEmpty() ||
            intent.action == Intents.ACTION_ASSISTANT_START_TIMER
        ) {
            intent.removeExtra(Intents.EXTRA_ASSISTANT_START_ISSUE_KEY)
            if (!startIssueKey.isNullOrEmpty()) {
                startTimerForIssueKey(startIssueKey)
            }
        }

        if (intent.action == Intents.ACTION_ASSISTANT_STOP_TIMER) {
            openStopDialogForActiveTimer()
        }
    }

    private fun openStopDialogForActiveTimer() {
        lifecycleScope.launch {
            val worklogId =
                withContext(Dispatchers.IO) {
                    val user = userRepository.getActiveUser() ?: return@withContext null
                    database.worklogDao().getActiveWorklogForUser(user.id)?.workId
                }
            if (worklogId != null) {
                pendingStopWorklogId = worklogId
            } else {
                Timber.i("Assistant stop timer: no active worklog")
            }
        }
    }

    private fun startTimerForIssueKey(rawKey: String) {
        lifecycleScope.launch {
            val result =
                withContext(Dispatchers.IO) {
                    resolveAndStartTimer(rawKey)
                }
            when (result) {
                is StartTimerResult.Started -> {
                    pendingOpenFeature = Intents.FEATURE_ISSUES
                    if (result.closedPreviousWorklogId != null) {
                        pendingEditorWorklogId = result.closedPreviousWorklogId
                    }
                }
                is StartTimerResult.NotFound -> {
                    pendingSearchQuery = rawKey
                    pendingOpenFeature = Intents.FEATURE_ISSUES
                    Timber.i("Assistant start timer: issue not found for %s; opening search", rawKey)
                }
                is StartTimerResult.NoUser -> {
                    Timber.i("Assistant start timer: no active user")
                }
            }
        }
    }

    private suspend fun resolveAndStartTimer(rawKey: String): StartTimerResult {
        val user = userRepository.getActiveUser() ?: return StartTimerResult.NoUser
        val key = rawKey.trim()
        if (key.isEmpty()) return StartTimerResult.NotFound

        val local =
            if (database.issueDao().issueExists(user.id, key) > 0) {
                database.issueDao().findIssueById(user.id, key)
            } else {
                null
            }

        val issue =
            local
                ?: runCatching {
                    jiraRepository
                        .searchIssuesRemote(key)
                        .firstOrNull { it.id.value.equals(key, ignoreCase = true) }
                }.getOrNull()
                ?: runCatching {
                    jiraRepository
                        .searchIssuesRemote(key)
                        .firstOrNull()
                }.getOrNull()

        if (issue == null) {
            return StartTimerResult.NotFound
        }

        val closedId = jiraRepository.startLogFor(IssueId(issue.id.value))
        val broadcast =
            Intent(Intents.ACTION_WORKLOG_CHANGED).setPackage(packageName)
        sendBroadcast(broadcast, "dae.aevum.permission.WORKLOG_CHANGED")
        widgetRefresher.refreshAll()
        return StartTimerResult.Started(closedPreviousWorklogId = closedId)
    }

    private sealed class StartTimerResult {
        data class Started(
            val closedPreviousWorklogId: Long?,
        ) : StartTimerResult()

        data object NotFound : StartTimerResult()

        data object NoUser : StartTimerResult()
    }
}
