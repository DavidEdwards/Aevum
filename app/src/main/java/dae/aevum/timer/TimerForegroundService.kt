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
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import dae.aevum.MainActivity
import dae.aevum.domain.preferences.TimerReminderPreferences
import dae.aevum.domain.preferences.TimerReminderSettings
import dae.aevum.domain.repositories.JiraRepository
import dae.aevum.domain.repositories.WidgetRefresher
import dae.aevum.utils.Intents
import dae.aevum.utils.TimerReminderHelpers
import dae.aevum.utils.TimerReminderVibration
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import dae.aevum.common.R as CommonR

@AndroidEntryPoint
class TimerForegroundService : Service() {
    @Inject
    lateinit var jiraRepository: JiraRepository

    @Inject
    lateinit var widgetRefresher: WidgetRefresher

    @Inject
    lateinit var timerReminderPreferences: TimerReminderPreferences

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var vibrationJob: Job? = null

    private var widgetRefreshJob: Job? = null
    private var observeJob: Job? = null
    private var currentWorklogId: Long? = null

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
        when (intent?.action) {
            Intents.ACTION_TIMER_STOP -> {
                serviceScope.launch { handleStopAction() }
                return START_STICKY
            }

            Intents.ACTION_TIMER_ADD_15 -> {
                serviceScope.launch { adjustActiveTime(+FIFTEEN_MIN_MS) }
                return START_STICKY
            }

            Intents.ACTION_TIMER_SUB_15 -> {
                serviceScope.launch { adjustActiveTime(-FIFTEEN_MIN_MS) }
                return START_STICKY
            }
        }

        ensureForeground(
            issueId = intent?.getStringExtra(EXTRA_ISSUE_ID).orEmpty(),
            issueTitle = intent?.getStringExtra(EXTRA_ISSUE_TITLE).orEmpty(),
            startedAtEpochMs =
                intent?.getLongExtra(EXTRA_STARTED_AT, System.currentTimeMillis())
                    ?: System.currentTimeMillis(),
            worklogId = intent?.getLongExtra(EXTRA_WORKLOG_ID, -1L)?.takeIf { it > 0 },
        )

        if (observeJob == null) {
            observeJob =
                serviceScope.launch {
                    val activeTimer =
                        jiraRepository
                            .flowActiveIssue()
                            .map { active ->
                                active?.let {
                                    TimerState(
                                        worklogId = it.worklog.workId,
                                        issueId = it.issue.id.value,
                                        issueTitle = it.issue.title,
                                        startedAt = it.worklog.from,
                                    )
                                }
                            }.distinctUntilChanged()
                    combine(
                        activeTimer,
                        timerReminderPreferences.settings,
                    ) { state, reminder ->
                        state to reminder
                    }.collect { (state, reminder) ->
                        if (state == null) {
                            Timber.v("No active timer; stopping foreground service")
                            currentWorklogId = null
                            stopWidgetRefreshLoop()
                            stopSelfSafely()
                        } else {
                            currentWorklogId = state.worklogId
                            ensureForeground(
                                issueId = state.issueId,
                                issueTitle = state.issueTitle,
                                startedAtEpochMs = state.startedAt.toEpochMilli(),
                                worklogId = state.worklogId,
                            )
                            restartVibrationLoop(state.startedAt, reminder)
                            restartWidgetRefreshLoop()
                        }
                    }
                }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        vibrationJob?.cancel()
        widgetRefreshJob?.cancel()
        observeJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    private suspend fun handleStopAction() {
        val active = jiraRepository.flowActiveIssue().firstOrNull() ?: return
        val openIntent =
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra(Intents.EXTRA_OPEN_STOP_WORKLOG_ID, active.worklog.workId)
            }
        startActivity(openIntent)
    }

    private suspend fun adjustActiveTime(deltaMs: Long) {
        val active = jiraRepository.flowActiveIssue().firstOrNull() ?: return
        val worklogId = active.worklog.workId
        if (deltaMs > 0) {
            jiraRepository.incrementWorklogTime(worklogId, deltaMs)
        } else {
            jiraRepository.decrementWorklogTime(worklogId, -deltaMs)
        }
        widgetRefresher.refreshAll()
    }

    private fun ensureForeground(
        issueId: String,
        issueTitle: String,
        startedAtEpochMs: Long,
        worklogId: Long?,
    ) {
        if (worklogId != null) currentWorklogId = worklogId
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

        val stopIntent =
            Intent(this, TimerForegroundService::class.java).apply {
                action = Intents.ACTION_TIMER_STOP
            }
        val addIntent =
            Intent(this, TimerForegroundService::class.java).apply {
                action = Intents.ACTION_TIMER_ADD_15
            }
        val subIntent =
            Intent(this, TimerForegroundService::class.java).apply {
                action = Intents.ACTION_TIMER_SUB_15
            }

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
            .addAction(
                0,
                getString(CommonR.string.timer_action_sub_15),
                PendingIntent.getService(
                    this,
                    1,
                    subIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            ).addAction(
                0,
                getString(CommonR.string.timer_action_add_15),
                PendingIntent.getService(
                    this,
                    2,
                    addIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            ).addAction(
                0,
                getString(CommonR.string.timer_action_stop),
                PendingIntent.getService(
                    this,
                    3,
                    stopIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            ).setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun restartVibrationLoop(
        startedAt: Instant,
        reminder: TimerReminderSettings,
    ) {
        vibrationJob?.cancel()
        if (!reminder.enabled) {
            vibrationJob = null
            return
        }
        vibrationJob =
            serviceScope.launch {
                while (isActive) {
                    val elapsedMs =
                        Duration
                            .between(startedAt, Instant.now())
                            .toMillis()
                            .coerceAtLeast(0L)
                    val delayMs =
                        TimerReminderHelpers.delayUntilNextMilestoneMs(
                            elapsedMs = elapsedMs,
                            intervalMs = reminder.intervalMs,
                        )
                    delay(delayMs)
                    vibrateReminder(reminder)
                }
            }
    }

    private fun restartWidgetRefreshLoop() {
        if (widgetRefreshJob?.isActive == true) return
        widgetRefreshJob =
            serviceScope.launch {
                while (isActive) {
                    try {
                        widgetRefresher.refreshAll()
                    } catch (t: Throwable) {
                        Timber.w(t, "Failed to refresh home screen widget")
                    }
                    delay(WIDGET_REFRESH_INTERVAL_MS)
                }
            }
    }

    private fun stopWidgetRefreshLoop() {
        widgetRefreshJob?.cancel()
        widgetRefreshJob = null
    }

    private fun vibrateReminder(reminder: TimerReminderSettings) {
        try {
            val vibrator =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val manager = getSystemService(VibratorManager::class.java)
                    manager.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    getSystemService(VIBRATOR_SERVICE) as Vibrator
                }
            val shortPulse =
                TimerReminderSettings.clampDurationMs(reminder.vibrationDurationMs)
            val timings = reminder.pattern.timings(shortPulse)
            vibrator.vibrate(
                TimerReminderVibration.createEffect(
                    timings = timings,
                    amplitude = reminder.intensity.amplitude,
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
        stopWidgetRefreshLoop()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private data class TimerState(
        val worklogId: Long,
        val issueId: String,
        val issueTitle: String,
        val startedAt: Instant,
    )

    companion object {
        private const val CHANNEL_ID = "active_timer"
        private const val NOTIFICATION_ID = 1001
        private const val FIFTEEN_MIN_MS = 15L * 60L * 1000L
        private const val WIDGET_REFRESH_INTERVAL_MS = 60L * 1000L

        private const val EXTRA_ISSUE_ID = "issue_id"
        private const val EXTRA_ISSUE_TITLE = "issue_title"
        private const val EXTRA_STARTED_AT = "started_at"
        private const val EXTRA_WORKLOG_ID = "worklog_id"

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
