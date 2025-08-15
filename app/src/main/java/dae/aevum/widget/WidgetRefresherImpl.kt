package dae.aevum.widget

import android.content.Context
import android.content.Intent
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import dae.aevum.domain.repositories.WidgetRefresher
import dae.aevum.utils.Intents
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import timber.log.Timber

class WidgetRefresherImpl @Inject constructor(
    @ApplicationContext private val appContext: Context
) : WidgetRefresher {
    override suspend fun refreshAll() {
        Timber.d("WidgetRefresherImpl.refreshAll")
        val manager = GlanceAppWidgetManager(appContext)
        val ids = manager.getGlanceIds(AevumAppWidget::class.java)
        val key = intPreferencesKey("nonce")
        for (id in ids) {
            Timber.d("WidgetRefresherImpl: touching state for id=%s", id)
            updateAppWidgetState(appContext, id) { prefs ->
                val old = prefs[key] ?: 0
                prefs[key] = old + 1
            }
            Timber.d("WidgetRefresherImpl: requesting update for id=%s", id)
            AevumAppWidget().update(appContext, id)
        }
        val intent = Intent(Intents.ACTION_WORKLOG_CHANGED)
            .setClass(appContext, WorklogChangedReceiver::class.java)
        Timber.d("WidgetRefresherImpl: broadcasting worklog changed to receiver")
        appContext.sendBroadcast(intent, "dae.aevum.permission.WORKLOG_CHANGED")
    }
}


