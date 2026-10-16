package dae.aevum.assistant

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import dae.aevum.MainActivity
import dae.aevum.R
import dae.aevum.common.R as CommonR
import dae.aevum.database.entities.IssueEntity
import dae.aevum.domain.repositories.JiraRepository
import dae.aevum.utils.Intents
import dae.aevum.utils.WearRecentIssue
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pushes pinned and recently logged issues as dynamic shortcuts so Assistant
 * can suggest "Start timer on PROJ-123".
 */
@Singleton
class AssistantShortcutsSync
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val jiraRepository: JiraRepository,
    ) {
        fun start(scope: CoroutineScope) {
            scope.launch {
                combine(
                    jiraRepository.flowPinnedIssues(),
                    jiraRepository.flowRecentlyLoggedIssues(RECENT_LIMIT),
                ) { pinned, recent ->
                    mergeCandidates(pinned, recent)
                }.distinctUntilChanged()
                    .collect { candidates ->
                        publish(candidates)
                    }
            }
        }

        private fun publish(candidates: List<ShortcutCandidate>) {
            val shortcuts =
                candidates.map { candidate ->
                    val label =
                        context.getString(
                            R.string.app_actions_start_timer_shortcut,
                            candidate.issueId,
                        )
                    val intent =
                        Intent(context, MainActivity::class.java).apply {
                            action = Intents.ACTION_ASSISTANT_START_TIMER
                            putExtra(Intents.EXTRA_ASSISTANT_START_ISSUE_KEY, candidate.issueId)
                            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                    ShortcutInfoCompat
                        .Builder(context, shortcutId(candidate.issueId))
                        .setShortLabel(candidate.issueId.take(MAX_SHORT_LABEL))
                        .setLongLabel(label.take(MAX_LONG_LABEL))
                        .setIcon(
                            IconCompat.createWithResource(
                                context,
                                CommonR.drawable.ic_launcher_foreground,
                            ),
                        ).setIntent(intent)
                        .addCapabilityBinding(
                            CAPABILITY_START_TIMER,
                            PARAM_ISSUE_KEY,
                            listOf(candidate.issueId, candidate.title),
                        ).build()
                }

            ShortcutManagerCompat.removeAllDynamicShortcuts(context)
            shortcuts.forEach { shortcut ->
                ShortcutManagerCompat.pushDynamicShortcut(context, shortcut)
            }
            Timber.v("Assistant dynamic shortcuts published count=%s", shortcuts.size)
        }

        companion object {
            private const val CAPABILITY_START_TIMER = "custom.actions.intent.START_TIMER"
            private const val PARAM_ISSUE_KEY = "issueKey"
            private const val RECENT_LIMIT = 5
            private const val MAX_SHORTCUTS = 10
            private const val MAX_SHORT_LABEL = 25
            private const val MAX_LONG_LABEL = 50

            internal fun shortcutId(issueId: String): String = "start_timer_$issueId"

            internal fun mergeCandidates(
                pinned: List<IssueEntity>,
                recent: List<WearRecentIssue>,
            ): List<ShortcutCandidate> {
                val result = LinkedHashMap<String, ShortcutCandidate>()
                pinned.forEach { issue ->
                    result[issue.id.value] =
                        ShortcutCandidate(
                            issueId = issue.id.value,
                            title = issue.title,
                        )
                }
                recent.forEach { issue ->
                    result.putIfAbsent(
                        issue.issueId,
                        ShortcutCandidate(
                            issueId = issue.issueId,
                            title = issue.issueTitle,
                        ),
                    )
                }
                return result.values.take(MAX_SHORTCUTS)
            }
        }
    }

internal data class ShortcutCandidate(
    val issueId: String,
    val title: String,
)
