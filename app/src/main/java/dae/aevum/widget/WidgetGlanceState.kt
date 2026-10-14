package dae.aevum.widget

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.appwidget.state.updateAppWidgetState
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Glance only reloads [Preferences] when [androidx.glance.appwidget.GlanceAppWidget.update] runs
 * against an already-active session — it does **not** re-run `provideGlance`. Display data must
 * live in these preferences and be read via `currentState` inside `provideContent`.
 */
object WidgetGlanceState {
    val ACTIVE = booleanPreferencesKey("active")
    val WORKLOG_ID = longPreferencesKey("worklog_id")
    val ISSUE_ID = stringPreferencesKey("issue_id")
    val ISSUE_TITLE = stringPreferencesKey("issue_title")
    val STARTED_AT = longPreferencesKey("started_at")
    val HAS_SUGGESTION = booleanPreferencesKey("has_suggestion")
    val SUGGESTION_ID = stringPreferencesKey("suggestion_id")
    val SUGGESTION_TITLE = stringPreferencesKey("suggestion_title")

    fun write(
        prefs: MutablePreferences,
        state: WidgetUiState,
    ) {
        when (state) {
            is WidgetUiState.Active -> {
                prefs[ACTIVE] = true
                prefs[WORKLOG_ID] = state.active.worklogId
                prefs[ISSUE_ID] = state.active.issueId
                prefs[ISSUE_TITLE] = state.active.issueTitle
                prefs[STARTED_AT] = state.active.startedAtEpochMs
                prefs[HAS_SUGGESTION] = false
                prefs.remove(SUGGESTION_ID)
                prefs.remove(SUGGESTION_TITLE)
            }

            is WidgetUiState.Idle -> {
                prefs[ACTIVE] = false
                prefs.remove(WORKLOG_ID)
                prefs.remove(ISSUE_ID)
                prefs.remove(ISSUE_TITLE)
                prefs.remove(STARTED_AT)
                val suggestion = state.suggestion
                if (suggestion != null) {
                    prefs[HAS_SUGGESTION] = true
                    prefs[SUGGESTION_ID] = suggestion.issueId
                    prefs[SUGGESTION_TITLE] = suggestion.issueTitle
                } else {
                    prefs[HAS_SUGGESTION] = false
                    prefs.remove(SUGGESTION_ID)
                    prefs.remove(SUGGESTION_TITLE)
                }
            }
        }
    }

    fun read(prefs: Preferences): WidgetUiState {
        if (prefs[ACTIVE] == true) {
            val worklogId = prefs[WORKLOG_ID] ?: return WidgetUiState.Idle(null)
            val issueId = prefs[ISSUE_ID].orEmpty()
            if (issueId.isBlank() || worklogId <= 0L) return WidgetUiState.Idle(null)
            return WidgetUiState.Active(
                WidgetActiveState(
                    worklogId = worklogId,
                    issueId = issueId,
                    issueTitle = prefs[ISSUE_TITLE].orEmpty(),
                    startedAtEpochMs = prefs[STARTED_AT] ?: System.currentTimeMillis(),
                ),
            )
        }
        if (prefs[HAS_SUGGESTION] == true) {
            val suggestionId = prefs[SUGGESTION_ID].orEmpty()
            if (suggestionId.isNotBlank()) {
                return WidgetUiState.Idle(
                    WidgetIdleState(
                        issueId = suggestionId,
                        issueTitle = prefs[SUGGESTION_TITLE].orEmpty(),
                    ),
                )
            }
        }
        return WidgetUiState.Idle(null)
    }

    suspend fun loadFromDatabase(context: Context): WidgetUiState {
        val entryPoint =
            EntryPointAccessors.fromApplication(
                context,
                AevumWidgetEntryPoint::class.java,
            )
        val userRepo = entryPoint.userRepository()
        val db = entryPoint.database()
        return try {
            withContext(Dispatchers.IO) {
                val user = userRepo.getActiveUser() ?: return@withContext WidgetUiState.Idle(null)
                val activeWork = db.worklogDao().getActiveWorklogForUser(user.id)
                if (activeWork != null) {
                    val issue =
                        try {
                            db.issueDao().findIssueById(user.id, activeWork.issueId.value)
                        } catch (_: Throwable) {
                            null
                        }
                    if (issue != null) {
                        return@withContext WidgetUiState.Active(
                            WidgetActiveState(
                                worklogId = activeWork.workId,
                                issueId = issue.id.value,
                                issueTitle = issue.title,
                                startedAtEpochMs = activeWork.from.toEpochMilli(),
                            ),
                        )
                    }
                }
                val lastCompleted = db.worklogDao().getLastCompletedWorklogForUser(user.id)
                val lastIssue =
                    lastCompleted?.let {
                        try {
                            db.issueDao().findIssueById(user.id, it.issueId.value)
                        } catch (_: Throwable) {
                            null
                        }
                    }
                val pinned = db.issueDao().getFirstPinnedIssue(user.id)
                WidgetUiState.Idle(
                    WidgetIdleResolver.resolve(
                        lastCompleted = lastCompleted,
                        lastCompletedIssue = lastIssue,
                        firstPinned = pinned,
                    ),
                )
            }
        } catch (_: Throwable) {
            WidgetUiState.Idle(null)
        }
    }

    suspend fun syncFromDatabase(
        context: Context,
        glanceId: GlanceId,
    ) {
        val state = loadFromDatabase(context)
        updateAppWidgetState(context, glanceId) { prefs ->
            write(prefs, state)
        }
    }

    suspend fun writeStartedAt(
        context: Context,
        glanceId: GlanceId,
        startedAtEpochMs: Long,
    ) {
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[ACTIVE] = true
            prefs[STARTED_AT] = startedAtEpochMs
        }
    }
}

sealed interface WidgetUiState {
    data class Active(
        val active: WidgetActiveState,
    ) : WidgetUiState

    data class Idle(
        val suggestion: WidgetIdleState?,
    ) : WidgetUiState
}
