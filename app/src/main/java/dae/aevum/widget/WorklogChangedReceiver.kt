package dae.aevum.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dae.aevum.utils.Intents
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class WorklogChangedReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != Intents.ACTION_WORKLOG_CHANGED) return
        Timber.d("WorklogChangedReceiver.onReceive")
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val refresher =
                    EntryPointAccessors
                        .fromApplication(
                            context.applicationContext,
                            AevumWidgetEntryPoint::class.java,
                        ).widgetRefresher()
                refresher.refreshAll()
            } finally {
                pending.finish()
            }
        }
    }
}
