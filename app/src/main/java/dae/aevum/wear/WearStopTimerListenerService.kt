package dae.aevum.wear

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import dae.aevum.domain.repositories.JiraRepository
import dae.aevum.domain.repositories.WidgetRefresher
import dae.aevum.utils.IssueId
import dae.aevum.utils.WearActiveTimerContract
import dae.aevum.utils.WearStartTimerCodec
import dae.aevum.utils.WearStopTimerCodec
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class WearStopTimerListenerService : WearableListenerService() {
    @Inject
    lateinit var jiraRepository: JiraRepository

    @Inject
    lateinit var widgetRefresher: WidgetRefresher

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(messageEvent: MessageEvent) {
        when (messageEvent.path) {
            WearActiveTimerContract.STOP_PATH -> handleStop(messageEvent.data)
            WearActiveTimerContract.START_PATH -> handleStart(messageEvent.data)
            else -> Timber.v("Ignoring Wear message path=%s", messageEvent.path)
        }
    }

    private fun handleStop(payload: ByteArray) {
        val request = WearStopTimerCodec.decode(payload)
        if (request == null) {
            Timber.w("Ignoring invalid Wear stop-timer payload")
            return
        }
        Timber.d(
            "Wear stop-timer: worklogId=%s summaryLen=%d",
            request.worklogId,
            request.summary.length,
        )
        scope.launch {
            try {
                jiraRepository.stopLoggingFor(request.worklogId, request.summary)
                widgetRefresher.refreshAll()
            } catch (t: Throwable) {
                Timber.e(t, "Failed to stop timer from Wear")
            }
        }
    }

    private fun handleStart(payload: ByteArray) {
        val request = WearStartTimerCodec.decode(payload)
        if (request == null) {
            Timber.w("Ignoring invalid Wear start-timer payload")
            return
        }
        Timber.d("Wear start-timer: issueId=%s", request.issueId)
        scope.launch {
            try {
                jiraRepository.startLogFor(IssueId(request.issueId))
                widgetRefresher.refreshAll()
            } catch (t: Throwable) {
                Timber.e(t, "Failed to start timer from Wear")
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
