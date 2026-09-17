package dae.aevum.wear.data

import android.content.Context
import com.google.android.gms.wearable.Wearable
import dae.aevum.utils.WearActiveTimerContract
import dae.aevum.utils.WearStopTimerCodec
import dae.aevum.utils.WearStopTimerRequest
import kotlinx.coroutines.tasks.await
import timber.log.Timber

object StopTimerMessenger {
    suspend fun send(
        context: Context,
        worklogId: Long,
        summary: String,
    ): Boolean {
        val nodes = Wearable.getNodeClient(context).connectedNodes.await()
        if (nodes.isEmpty()) {
            Timber.w("No connected phone nodes to send stop-timer")
            return false
        }
        val payload =
            WearStopTimerCodec.encode(
                WearStopTimerRequest(worklogId = worklogId, summary = summary),
            )
        var anySuccess = false
        for (node in nodes) {
            try {
                Wearable
                    .getMessageClient(context)
                    .sendMessage(node.id, WearActiveTimerContract.STOP_PATH, payload)
                    .await()
                anySuccess = true
                Timber.d("Sent stop-timer to node=%s worklogId=%s", node.displayName, worklogId)
            } catch (t: Throwable) {
                Timber.w(t, "Failed to send stop-timer to node=%s", node.displayName)
            }
        }
        return anySuccess
    }
}
