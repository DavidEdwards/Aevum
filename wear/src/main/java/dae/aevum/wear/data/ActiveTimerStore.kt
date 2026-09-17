package dae.aevum.wear.data

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable
import dae.aevum.utils.ActiveTimerSnapshot
import dae.aevum.utils.WearActiveTimerContract
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import timber.log.Timber

private val Context.activeTimerDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "active_timer",
)

class ActiveTimerStore(
    private val context: Context,
) {
    private val activeKey = booleanPreferencesKey(WearActiveTimerContract.KEY_ACTIVE)
    private val issueIdKey = stringPreferencesKey(WearActiveTimerContract.KEY_ISSUE_ID)
    private val issueTitleKey = stringPreferencesKey(WearActiveTimerContract.KEY_ISSUE_TITLE)
    private val worklogIdKey = longPreferencesKey(WearActiveTimerContract.KEY_WORKLOG_ID)
    private val startedAtKey = longPreferencesKey(WearActiveTimerContract.KEY_STARTED_AT_EPOCH_MS)

    val snapshots: Flow<ActiveTimerSnapshot?> =
        context.activeTimerDataStore.data.map { prefs ->
            if (prefs[activeKey] != true) {
                null
            } else {
                ActiveTimerSnapshot(
                    issueId = prefs[issueIdKey].orEmpty(),
                    issueTitle = prefs[issueTitleKey].orEmpty(),
                    startedAtEpochMs = prefs[startedAtKey] ?: 0L,
                    worklogId = prefs[worklogIdKey] ?: 0L,
                ).takeIf { it.issueId.isNotBlank() && it.worklogId > 0L }
            }
        }

    suspend fun write(snapshot: ActiveTimerSnapshot?) {
        context.activeTimerDataStore.edit { prefs ->
            if (snapshot == null) {
                prefs[activeKey] = false
                prefs.remove(issueIdKey)
                prefs.remove(issueTitleKey)
                prefs.remove(worklogIdKey)
                prefs.remove(startedAtKey)
            } else {
                prefs[activeKey] = true
                prefs[issueIdKey] = snapshot.issueId
                prefs[issueTitleKey] = snapshot.issueTitle
                prefs[worklogIdKey] = snapshot.worklogId
                prefs[startedAtKey] = snapshot.startedAtEpochMs
            }
        }
    }

    suspend fun readCached(): ActiveTimerSnapshot? = snapshots.first()

    suspend fun refreshFromDataLayer(): ActiveTimerSnapshot? {
        return try {
            val uri =
                Uri
                    .Builder()
                    .scheme(PutDataRequest.WEAR_URI_SCHEME)
                    .path(WearActiveTimerContract.PATH)
                    .build()
            val buffer = Wearable.getDataClient(context).getDataItems(uri).await()
            try {
                val item =
                    buffer.firstOrNull() ?: return readCached().also {
                        Timber.v("No Data Layer item for active timer; using cache")
                    }
                val map = DataMapItem.fromDataItem(item).dataMap
                val snapshot =
                    fromDataMap(
                        active = map.getBoolean(WearActiveTimerContract.KEY_ACTIVE, false),
                        issueId = map.getString(WearActiveTimerContract.KEY_ISSUE_ID),
                        issueTitle = map.getString(WearActiveTimerContract.KEY_ISSUE_TITLE),
                        startedAtEpochMs =
                            map.getLong(WearActiveTimerContract.KEY_STARTED_AT_EPOCH_MS, 0L),
                        worklogId = map.getLong(WearActiveTimerContract.KEY_WORKLOG_ID, 0L),
                    )
                write(snapshot)
                snapshot
            } finally {
                buffer.release()
            }
        } catch (t: Throwable) {
            Timber.w(t, "Failed to read active timer from Data Layer")
            readCached()
        }
    }

    companion object {
        fun fromDataMap(
            active: Boolean,
            issueId: String?,
            issueTitle: String?,
            startedAtEpochMs: Long,
            worklogId: Long,
        ): ActiveTimerSnapshot? {
            if (!active) return null
            val id = issueId.orEmpty()
            if (id.isBlank() || worklogId <= 0L) return null
            return ActiveTimerSnapshot(
                issueId = id,
                issueTitle = issueTitle.orEmpty(),
                startedAtEpochMs = startedAtEpochMs,
                worklogId = worklogId,
            )
        }
    }
}
