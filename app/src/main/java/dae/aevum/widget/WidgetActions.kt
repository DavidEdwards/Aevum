package dae.aevum.widget

import android.content.Context
import android.content.Intent
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import dae.aevum.MainActivity
import dae.aevum.utils.Intents
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class StartSuggestedIssueAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        Timber.d("StartSuggestedIssueAction.onAction(glanceId=%s)", glanceId)
        val entryPoint =
            EntryPointAccessors.fromApplication(
                context,
                AevumWidgetEntryPoint::class.java,
            )
        val repo = entryPoint.jiraRepository()
        val userRepo = entryPoint.userRepository()
        val db = entryPoint.database()

        withContext(Dispatchers.IO) {
            val user = userRepo.getActiveUser() ?: return@withContext
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
            val issueId =
                when {
                    lastCompleted != null && lastIssue != null -> lastCompleted.issueId
                    pinned != null -> pinned.id
                    else -> return@withContext
                }
            val closedId = repo.startLogFor(issueId)
            if (closedId != null) {
                val editorIntent =
                    Intent(context, MainActivity::class.java).apply {
                        flags =
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                                Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra(Intents.EXTRA_OPEN_WORKLOG_EDITOR_ID, closedId)
                    }
                context.startActivity(editorIntent)
            }
        }
        // Full sync: active timer (or idle) must replace suggestion in preferences.
        WidgetGlanceState.syncFromDatabase(context.applicationContext, glanceId)
        AevumAppWidget().update(context.applicationContext, glanceId)
    }
}

class AddFifteenAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        adjustActiveTime(context, glanceId, +FIFTEEN_MIN_MS)
    }
}

class SubtractFifteenAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        adjustActiveTime(context, glanceId, -FIFTEEN_MIN_MS)
    }
}

private suspend fun adjustActiveTime(
    context: Context,
    glanceId: GlanceId,
    deltaMs: Long,
) {
    Timber.d("adjustActiveTime deltaMs=%s glanceId=%s", deltaMs, glanceId)
    val appContext = context.applicationContext
    val entryPoint =
        EntryPointAccessors.fromApplication(
            appContext,
            AevumWidgetEntryPoint::class.java,
        )
    val repo = entryPoint.jiraRepository()
    val userRepo = entryPoint.userRepository()
    val db = entryPoint.database()

    val newStartedAt =
        withContext(Dispatchers.IO) {
            val user = userRepo.getActiveUser() ?: return@withContext null
            val active = db.worklogDao().getActiveWorklogForUser(user.id) ?: return@withContext null
            if (deltaMs > 0) {
                repo.incrementWorklogTime(active.workId, deltaMs)
            } else {
                repo.decrementWorklogTime(active.workId, -deltaMs)
            }
            db.worklogDao().findWorkById(active.workId)?.from?.toEpochMilli()
        }

    if (newStartedAt != null) {
        // Write into Glance preferences first. If a session is already running, update() only
        // reloads preferences — it will not re-query the database.
        WidgetGlanceState.writeStartedAt(appContext, glanceId, newStartedAt)
    } else {
        WidgetGlanceState.syncFromDatabase(appContext, glanceId)
    }
    AevumAppWidget().update(appContext, glanceId)
}

private const val FIFTEEN_MIN_MS = 15L * 60L * 1000L
