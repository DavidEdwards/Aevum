package dae.aevum.wear.data

import android.content.Context
import com.google.android.gms.wearable.Wearable
import dae.aevum.utils.WearActiveTimerContract
import dae.aevum.utils.WearStartTimerCodec
import dae.aevum.utils.WearStartTimerRequest
import kotlinx.coroutines.tasks.await
import timber.log.Timber

object StartTimerMessenger {
    suspend fun send(
        context: Context,
        issueId: String,
    ): Boolean {
        val nodes = Wearable.getNodeClient(context).connectedNodes.await()
        if (nodes.isEmpty()) {
            Timber.w("No connected phone nodes to send start-timer")
            return false
        }
        val payload =
            WearStartTimerCodec.encode(
                WearStartTimerRequest(issueId = issueId),
            )
        var anySuccess = false
        for (node in nodes) {
            try {
                Wearable
                    .getMessageClient(context)
                    .sendMessage(node.id, WearActiveTimerContract.START_PATH, payload)
                    .await()
                anySuccess = true
                Timber.d("Sent start-timer to node=%s issueId=%s", node.displayName, issueId)
            } catch (t: Throwable) {
                Timber.w(t, "Failed to send start-timer to node=%s", node.displayName)
            }
        }
        return anySuccess
    }
}
