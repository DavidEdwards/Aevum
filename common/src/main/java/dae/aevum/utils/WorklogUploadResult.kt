package dae.aevum.utils

/**
 * Result of bulk-uploading pending worklogs to Jira.
 */
sealed class WorklogUploadResult {
    data object Idle : WorklogUploadResult()

    data object Running : WorklogUploadResult()

    data class Success(
        val uploadedCount: Int,
    ) : WorklogUploadResult()

    data class PartialFailure(
        val uploadedCount: Int,
        val failedIssueKeys: List<String>,
    ) : WorklogUploadResult()

    data class Failed(
        val message: String? = null,
    ) : WorklogUploadResult()
}

data class TimesheetProgress(
    val todayHours: Double,
    val weekHours: Double,
)
