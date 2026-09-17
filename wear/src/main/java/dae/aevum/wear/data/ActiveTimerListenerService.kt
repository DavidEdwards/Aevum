package dae.aevum.wear.data

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import dae.aevum.utils.WearActiveTimerContract
import dae.aevum.wear.tile.ActiveTimerTileService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber
import androidx.wear.tiles.TileService

class ActiveTimerListenerService : WearableListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        val store = ActiveTimerStore(applicationContext)
        try {
            for (event in dataEvents) {
                val path = event.dataItem.uri.path ?: continue
                if (path != WearActiveTimerContract.PATH) continue
                when (event.type) {
                    DataEvent.TYPE_CHANGED -> {
                        val map = DataMapItem.fromDataItem(event.dataItem).dataMap
                        val snapshot =
                            ActiveTimerStore.fromDataMap(
                                active = map.getBoolean(WearActiveTimerContract.KEY_ACTIVE, false),
                                issueId = map.getString(WearActiveTimerContract.KEY_ISSUE_ID),
                                issueTitle = map.getString(WearActiveTimerContract.KEY_ISSUE_TITLE),
                                startedAtEpochMs =
                                    map.getLong(
                                        WearActiveTimerContract.KEY_STARTED_AT_EPOCH_MS,
                                        0L,
                                    ),
                                worklogId =
                                    map.getLong(WearActiveTimerContract.KEY_WORKLOG_ID, 0L),
                            )
                        Timber.d(
                            "Wear Data Layer update: active=%s issue=%s",
                            snapshot != null,
                            snapshot?.issueId,
                        )
                        scope.launch {
                            store.write(snapshot)
                            TileService
                                .getUpdater(applicationContext)
                                .requestUpdate(ActiveTimerTileService::class.java)
                        }
                    }
                    DataEvent.TYPE_DELETED -> {
                        scope.launch {
                            store.write(null)
                            TileService
                                .getUpdater(applicationContext)
                                .requestUpdate(ActiveTimerTileService::class.java)
                        }
                    }
                }
            }
        } finally {
            dataEvents.release()
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
