package dae.aevum

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dae.aevum.ui.composables.Router
import dae.aevum.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                Router()
            }
        }
    }
}