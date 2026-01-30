package dae.aevum.widget

import android.content.Context
import android.content.Intent
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import dae.aevum.utils.Intents
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class StopActiveTimerAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        Timber.d("StopActiveTimerAction.onAction(glanceId=%s)", glanceId)
        val entryPoint =
            EntryPointAccessors.fromApplication(
                context,
                AevumWidgetEntryPoint::class.java,
            )
        val repo = entryPoint.jiraRepository()
        val widgetRefresher = entryPoint.widgetRefresher()

        withContext(Dispatchers.IO) { repo.stopLogging("") }
        // touch glance state to invalidate composition
        updateAppWidgetState(context, glanceId) { prefs ->
            val key = intPreferencesKey("nonce")
            val old = prefs[key] ?: 0
            prefs[key] = old + 1
        }
        // Update specific id first for snappy feedback, then all as a fallback
        val appCtx = context.applicationContext
        AevumAppWidget().update(appCtx, glanceId)
        AevumAppWidget().updateAll(appCtx)

        widgetRefresher.refreshAll()

        Timber.d("StopActiveTimerAction: requested widget updates and broadcast")
        val explicit =
            Intent(Intents.ACTION_WORKLOG_CHANGED)
                .setClass(appCtx, WorklogChangedReceiver::class.java)
        context.sendBroadcast(explicit, "dae.aevum.permission.WORKLOG_CHANGED")
    }
}

class StartLastIssueAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        Timber.d("StartLastIssueAction.onAction(glanceId=%s)", glanceId)
        val entryPoint =
            EntryPointAccessors.fromApplication(
                context,
                AevumWidgetEntryPoint::class.java,
            )
        val repo = entryPoint.jiraRepository()
        val userRepo = entryPoint.userRepository()
        val db = entryPoint.database()
        val widgetRefresher = entryPoint.widgetRefresher()

        withContext(Dispatchers.IO) {
            val user = userRepo.getActiveUser() ?: return@withContext
            val works = db.worklogDao().getAllWorks().filter { it.userId == user.id }
            val last = works.maxByOrNull { it.from } ?: return@withContext
            repo.startLogFor(last.issueId)
        }
        // touch glance state to invalidate composition
        updateAppWidgetState(context, glanceId) { prefs ->
            val key = intPreferencesKey("nonce")
            val old = prefs[key] ?: 0
            prefs[key] = old + 1
        }
        val appCtx = context.applicationContext
        AevumAppWidget().update(appCtx, glanceId)
        AevumAppWidget().updateAll(appCtx)

        widgetRefresher.refreshAll()

        Timber.d("StartLastIssueAction: requested widget updates and broadcast")
        val explicit =
            Intent(Intents.ACTION_WORKLOG_CHANGED)
                .setClass(appCtx, WorklogChangedReceiver::class.java)
        context.sendBroadcast(explicit, "dae.aevum.permission.WORKLOG_CHANGED")
    }
}
