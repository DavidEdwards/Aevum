package dae.aevum.utils

/**
 * Shared Data Layer / Message Client contract for syncing the active timer
 * and stopping it from Wear OS. Phone and wear apps must share the same
 * applicationId and signing key.
 */
object WearActiveTimerContract {
    const val PATH = "/active_timer"
    const val STOP_PATH = "/stop_timer"

    const val KEY_ACTIVE = "active"
    const val KEY_ISSUE_ID = "issue_id"
    const val KEY_ISSUE_TITLE = "issue_title"
    const val KEY_WORKLOG_ID = "worklog_id"
    const val KEY_STARTED_AT_EPOCH_MS = "started_at_epoch_ms"
    /** Forces Data Layer to treat the item as changed even when other fields match. */
    const val KEY_UPDATED_AT_EPOCH_MS = "updated_at_epoch_ms"
    const val KEY_SUMMARY = "summary"
}

data class ActiveTimerSnapshot(
    val issueId: String,
    val issueTitle: String,
    val startedAtEpochMs: Long,
    val worklogId: Long,
)

data class WearStopTimerRequest(
    val worklogId: Long,
    val summary: String,
)
