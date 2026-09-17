package dae.aevum.wear

import android.content.Context
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import dae.aevum.domain.repositories.WearActiveTimerSync
import dae.aevum.utils.ActiveTimerSnapshot
import dae.aevum.utils.WearActiveTimerContract
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WearActiveTimerSyncImpl
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) : WearActiveTimerSync {
        override suspend fun sync(active: ActiveTimerSnapshot?) {
            if (!isWearableApiAvailable()) {
                Timber.v("Wearable API unavailable; skipping active timer sync")
                return
            }
            try {
                val putRequest =
                    PutDataMapRequest.create(WearActiveTimerContract.PATH).apply {
                        dataMap.putBoolean(WearActiveTimerContract.KEY_ACTIVE, active != null)
                        dataMap.putString(
                            WearActiveTimerContract.KEY_ISSUE_ID,
                            active?.issueId.orEmpty(),
                        )
                        dataMap.putString(
                            WearActiveTimerContract.KEY_ISSUE_TITLE,
                            active?.issueTitle.orEmpty(),
                        )
                        dataMap.putLong(
                            WearActiveTimerContract.KEY_WORKLOG_ID,
                            active?.worklogId ?: 0L,
                        )
                        dataMap.putLong(
                            WearActiveTimerContract.KEY_STARTED_AT_EPOCH_MS,
                            active?.startedAtEpochMs ?: 0L,
                        )
                        dataMap.putLong(
                            WearActiveTimerContract.KEY_UPDATED_AT_EPOCH_MS,
                            System.currentTimeMillis(),
                        )
                    }.asPutDataRequest()
                        .setUrgent()

                Wearable.getDataClient(context).putDataItem(putRequest).await()
                Timber.d(
                    "Synced active timer to Wear: active=%s issue=%s",
                    active != null,
                    active?.issueId,
                )
            } catch (t: Throwable) {
                Timber.w(t, "Failed to sync active timer to Wear")
            }
        }

        private fun isWearableApiAvailable(): Boolean {
            val availability = GoogleApiAvailability.getInstance()
            return availability.isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
        }
    }
