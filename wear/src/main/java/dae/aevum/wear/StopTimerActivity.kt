package dae.aevum.wear

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.wear.tiles.TileService
import dae.aevum.wear.data.ActiveTimerStore
import dae.aevum.wear.data.StopTimerMessenger
import dae.aevum.wear.tile.ActiveTimerTileService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class StopTimerActivity : AppCompatActivity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var worklogId: Long = -1L
    private var issueId: String = ""
    private lateinit var statusView: TextView
    private lateinit var retryButton: Button
    private lateinit var stopAnywayButton: Button

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startSpeechRecognition()
            } else {
                showFallback(getString(R.string.mic_permission_required))
            }
        }

    private val speechLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val spoken =
                    result.data
                        ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                        ?.firstOrNull()
                        .orEmpty()
                finalizeStop(spoken)
            } else {
                showFallback(getString(R.string.speech_cancelled))
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        worklogId = intent.getLongExtra(EXTRA_WORKLOG_ID, -1L)
        issueId = intent.getStringExtra(EXTRA_ISSUE_ID).orEmpty()
        if (worklogId <= 0L) {
            Toast.makeText(this, R.string.no_active_timer, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        statusView =
            TextView(this).apply {
                textSize = 16f
                gravity = Gravity.CENTER
                text =
                    getString(
                        R.string.speak_summary_for,
                        issueId.ifBlank { getString(R.string.active_timer_tile_label) },
                    )
            }
        retryButton =
            Button(this).apply {
                text = getString(R.string.speak_again)
                setOnClickListener { ensureMicAndSpeak() }
                visibility = android.view.View.GONE
            }
        stopAnywayButton =
            Button(this).apply {
                text = getString(R.string.stop_without_summary)
                setOnClickListener { finalizeStop("") }
                visibility = android.view.View.GONE
            }

        setContentView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(24, 24, 24, 24)
                addView(statusView)
                addView(retryButton)
                addView(stopAnywayButton)
            },
        )

        ensureMicAndSpeak()
    }

    private fun ensureMicAndSpeak() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED -> startSpeechRecognition()
            else -> permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startSpeechRecognition() {
        statusView.text =
            getString(
                R.string.speak_summary_for,
                issueId.ifBlank { getString(R.string.active_timer_tile_label) },
            )
        retryButton.visibility = android.view.View.GONE
        stopAnywayButton.visibility = android.view.View.GONE
        val intent =
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(
                    RecognizerIntent.EXTRA_PROMPT,
                    getString(R.string.speak_worklog_prompt),
                )
            }
        try {
            speechLauncher.launch(intent)
        } catch (_: Throwable) {
            showFallback(getString(R.string.speech_unavailable))
        }
    }

    private fun showFallback(message: String) {
        statusView.text = message
        retryButton.visibility = android.view.View.VISIBLE
        stopAnywayButton.visibility = android.view.View.VISIBLE
    }

    private fun finalizeStop(summary: String) {
        statusView.text = getString(R.string.stopping_timer)
        retryButton.visibility = android.view.View.GONE
        stopAnywayButton.visibility = android.view.View.GONE
        scope.launch {
            val sent =
                withContext(Dispatchers.IO) {
                    StopTimerMessenger.send(
                        context = this@StopTimerActivity,
                        worklogId = worklogId,
                        summary = summary,
                    )
                }
            if (sent) {
                withContext(Dispatchers.IO) {
                    ActiveTimerStore(this@StopTimerActivity).write(null)
                }
                TileService
                    .getUpdater(this@StopTimerActivity)
                    .requestUpdate(ActiveTimerTileService::class.java)
                Toast
                    .makeText(this@StopTimerActivity, R.string.timer_stopped, Toast.LENGTH_SHORT)
                    .show()
                finish()
            } else {
                showFallback(getString(R.string.phone_not_connected))
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_WORKLOG_ID = "worklog_id"
        const val EXTRA_ISSUE_ID = "issue_id"
    }
}
