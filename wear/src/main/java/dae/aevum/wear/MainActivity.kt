package dae.aevum.wear

import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import dae.aevum.wear.data.ActiveTimerStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : Activity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val textView =
            TextView(this).apply {
                textSize = 16f
                setPadding(24, 24, 24, 24)
                text = getString(R.string.wear_status_idle)
            }
        setContentView(textView)

        scope.launch {
            ActiveTimerStore(this@MainActivity).snapshots.collectLatest { snapshot ->
                textView.text =
                    if (snapshot == null) {
                        getString(R.string.no_active_timer)
                    } else {
                        buildString {
                            append(snapshot.issueId)
                            if (snapshot.issueTitle.isNotBlank()) {
                                append('\n')
                                append(snapshot.issueTitle)
                            }
                        }
                    }
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
