package dae.aevum.timer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import dae.aevum.MainActivity
import dae.aevum.common.R as CommonR
import dae.aevum.domain.repositories.JiraRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

@AndroidEntryPoint
class TimerForegroundService : Service() {
    @Inject
    lateinit var jiraRepository: JiraRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var vibrationJob: Job? = null
    private var observeJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        ensureForeground(
            issueId = intent?.getStringExtra(EXTRA_ISSUE_ID).orEmpty(),
            issueTitle = intent?.getStringExtra(EXTRA_ISSUE_TITLE).orEmpty(),
            startedAtEpochMs =
                intent?.getLongExtra(EXTRA_STARTED_AT, System.currentTimeMillis())
                    ?: System.currentTimeMillis(),
        )

        if (observeJob == null) {
            observeJob =
                serviceScope.launch {
                    jiraRepository
                        .flowActiveIssue()
                        .map { active ->
                            active?.let {
                                TimerState(
                                    issueId = it.issue.id.value,
                                    issueTitle = it.issue.title,
                                    startedAt = it.worklog.from,
                                )
                            }
                        }.distinctUntilChanged()
                        .collect { state ->
                            if (state == null) {
                                Timber.v("No active timer; stopping foreground service")
                                stopSelfSafely()
                            } else {
                                ensureForeground(
                                    issueId = state.issueId,
                                    issueTitle = state.issueTitle,
                                    startedAtEpochMs = state.startedAt.toEpochMilli(),
                                )
                                restartVibrationLoop(state.startedAt)
                            }
                        }
                }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        vibrationJob?.cancel()
        observeJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun ensureForeground(
        issueId: String,
        issueTitle: String,
        startedAtEpochMs: Long,
    ) {
        val notification = buildNotification(issueId, issueTitle, startedAtEpochMs)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(
        issueId: String,
        issueTitle: String,
        startedAtEpochMs: Long,
    ): Notification {
        val openAppIntent =
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        val contentPendingIntent =
            PendingIntent.getActivity(
                this,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val title =
            if (issueId.isBlank()) {
                getString(CommonR.string.timer_notification_title_generic)
            } else {
                getString(CommonR.string.timer_notification_title, issueId)
            }
        val text =
            issueTitle.ifBlank {
                getString(CommonR.string.timer_notification_text)
            }

        return NotificationCompat
            .Builder(this, CHANNEL_ID)
            .setSmallIcon(CommonR.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setUsesChronometer(true)
            .setWhen(startedAtEpochMs)
            .setContentIntent(contentPendingIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun restartVibrationLoop(startedAt: Instant) {
        vibrationJob?.cancel()
        vibrationJob =
            serviceScope.launch {
                while (isActive) {
                    val elapsedMs =
                        Duration
                            .between(startedAt, Instant.now())
                            .toMillis()
                            .coerceAtLeast(0L)
                    val nextMilestoneMs =
                        ((elapsedMs / VIBRATION_INTERVAL_MS) + 1) * VIBRATION_INTERVAL_MS
                    val delayMs = (nextMilestoneMs - elapsedMs).coerceAtLeast(1L)
                    delay(delayMs)
                    vibrateShort()
                }
            }
    }

    private fun vibrateShort() {
        try {
            val vibrator =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val manager = getSystemService(VibratorManager::class.java)
                    manager.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    getSystemService(VIBRATOR_SERVICE) as Vibrator
                }
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    VIBRATION_DURATION_MS,
                    VibrationEffect.DEFAULT_AMPLITUDE,
                ),
            )
        } catch (e: Exception) {
            Timber.w(e, "Failed to vibrate for timer milestone")
        }
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel =
            NotificationChannel(
                CHANNEL_ID,
                getString(CommonR.string.timer_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = getString(CommonR.string.timer_notification_channel_description)
                setShowBadge(false)
            }
        manager.createNotificationChannel(channel)
    }

    private fun stopSelfSafely() {
        vibrationJob?.cancel()
        vibrationJob = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private data class TimerState(
        val issueId: String,
        val issueTitle: String,
        val startedAt: Instant,
    )

    companion object {
        private const val CHANNEL_ID = "active_timer"
        private const val NOTIFICATION_ID = 1001
        private const val VIBRATION_INTERVAL_MS = 30L * 60L * 1000L
        private const val VIBRATION_DURATION_MS = 120L

        private const val EXTRA_ISSUE_ID = "issue_id"
        private const val EXTRA_ISSUE_TITLE = "issue_title"
        private const val EXTRA_STARTED_AT = "started_at"

        fun start(
            context: Context,
            issueId: String,
            issueTitle: String,
            startedAtEpochMs: Long,
        ) {
            val intent =
                Intent(context, TimerForegroundService::class.java).apply {
                    putExtra(EXTRA_ISSUE_ID, issueId)
                    putExtra(EXTRA_ISSUE_TITLE, issueTitle)
                    putExtra(EXTRA_STARTED_AT, startedAtEpochMs)
                }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, TimerForegroundService::class.java))
        }
    }
}
