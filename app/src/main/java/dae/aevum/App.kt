package dae.aevum

import android.app.Application
import dae.aevum.domain.repositories.JiraRepository
import dae.aevum.timer.TimerForegroundService
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class App : Application() {
    @Inject
    lateinit var jiraRepository: JiraRepository

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
        observeActiveTimer()
    }

    private fun observeActiveTimer() {
        applicationScope.launch {
            jiraRepository
                .flowActiveIssue()
                .map { active ->
                    active?.let {
                        ActiveTimerInfo(
                            issueId = it.issue.id.value,
                            issueTitle = it.issue.title,
                            startedAtEpochMs = it.worklog.from.toEpochMilli(),
                        )
                    }
                }.distinctUntilChanged()
                .collect { info ->
                    if (info == null) {
                        Timber.v("Active timer cleared; stopping foreground service")
                        TimerForegroundService.stop(this@App)
                    } else {
                        Timber.v("Active timer for %s; starting foreground service", info.issueId)
                        TimerForegroundService.start(
                            context = this@App,
                            issueId = info.issueId,
                            issueTitle = info.issueTitle,
                            startedAtEpochMs = info.startedAtEpochMs,
                        )
                    }
                }
        }
    }

    private data class ActiveTimerInfo(
        val issueId: String,
        val issueTitle: String,
        val startedAtEpochMs: Long,
    )
}
