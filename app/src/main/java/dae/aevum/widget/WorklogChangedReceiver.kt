package dae.aevum.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import dae.aevum.utils.Intents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class WorklogChangedReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action == Intents.ACTION_WORKLOG_CHANGED) {
            Timber.d("WorklogChangedReceiver.onReceive: action=%s", intent.action)
            val pending = goAsync()
            CoroutineScope(Dispatchers.Default).launch {
                try {
                    val appContext = context.applicationContext
                    val manager = GlanceAppWidgetManager(appContext)
                    val ids = manager.getGlanceIds(AevumAppWidget::class.java)
                    Timber.d("WorklogChangedReceiver: updating %d widget(s)", ids.size)
                    val key = intPreferencesKey("nonce")
                    for (id in ids) {
                        updateAppWidgetState(appContext, id) { prefs ->
                            val old = prefs[key] ?: 0
                            prefs[key] = old + 1
                        }
                        Timber.d("WorklogChangedReceiver: updating widget id=%s", id)
                        AevumAppWidget().update(appContext, id)
                    }
                } finally {
                    pending.finish()
                }
            }
        }
    }
}
