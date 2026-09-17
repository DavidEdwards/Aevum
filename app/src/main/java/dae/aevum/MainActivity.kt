package dae.aevum

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dae.aevum.ui.composables.Router
import dae.aevum.ui.theme.AppTheme
import dae.aevum.utils.Intents
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var isReady = false
    private var pendingStopWorklogId by mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        splashScreen.setKeepOnScreenCondition { !isReady }
        consumeStopExtra(intent)

        enableEdgeToEdge()
        setContent {
            AppTheme {
                Router(
                    onReady = { isReady = true },
                    pendingStopWorklogId = pendingStopWorklogId,
                    onStopDialogOpened = { pendingStopWorklogId = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeStopExtra(intent)
    }

    private fun consumeStopExtra(intent: Intent?) {
        val id = intent?.getLongExtra(Intents.EXTRA_OPEN_STOP_WORKLOG_ID, -1L) ?: -1L
        if (id > 0) {
            pendingStopWorklogId = id
            intent?.removeExtra(Intents.EXTRA_OPEN_STOP_WORKLOG_ID)
        }
    }
}
