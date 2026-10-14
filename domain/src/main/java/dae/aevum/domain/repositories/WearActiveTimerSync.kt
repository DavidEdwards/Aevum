package dae.aevum.domain.repositories

import dae.aevum.utils.ActiveTimerSnapshot
import dae.aevum.utils.WearRecentIssue

/** Pushes timer state and recent issues to paired Wear OS devices via the Data Layer API. */
interface WearActiveTimerSync {
    suspend fun sync(active: ActiveTimerSnapshot?)

    suspend fun syncRecentIssues(issues: List<WearRecentIssue>)
}
