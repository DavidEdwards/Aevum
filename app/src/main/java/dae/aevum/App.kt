package dae.aevum

import android.app.Application
import dae.aevum.domain.repositories.JiraRepository
import dae.aevum.domain.repositories.WearActiveTimerSync
import dae.aevum.timer.TimerForegroundService
import dae.aevum.utils.ActiveTimerSnapshot
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

    @Inject
    lateinit var wearActiveTimerSync: WearActiveTimerSync

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
                        ActiveTimerSnapshot(
                            issueId = it.issue.id.value,
                            issueTitle = it.issue.title,
                            startedAtEpochMs = it.worklog.from.toEpochMilli(),
                            worklogId = it.worklog.workId,
                        )
                    }
                }.distinctUntilChanged()
                .collect { info ->
                    wearActiveTimerSync.sync(info)
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
}
