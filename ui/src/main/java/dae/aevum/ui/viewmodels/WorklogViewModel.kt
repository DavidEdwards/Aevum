package dae.aevum.ui.viewmodels

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dae.aevum.domain.preferences.TimesheetPreferences
import dae.aevum.domain.preferences.TimesheetTargets
import dae.aevum.domain.repositories.AudioRepository
import dae.aevum.domain.repositories.JiraRepository
import dae.aevum.domain.repositories.WidgetRefresher
import dae.aevum.ui.models.WorklogIssueUiModel
import dae.aevum.ui.models.WorklogUiModel
import dae.aevum.utils.IssueId
import dae.aevum.utils.TimesheetProgress
import dae.aevum.utils.WorklogTimeHelpers
import dae.aevum.utils.WorklogUploadResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WorklogViewModel
    @Inject
    constructor(
        private val savedStateHandle: SavedStateHandle,
        private val jiraRepository: JiraRepository,
        private val audioRepository: AudioRepository,
        @param:ApplicationContext private val appContext: Context,
        private val widgetRefresher: WidgetRefresher,
        private val timesheetPreferences: TimesheetPreferences,
    ) : ViewModel() {
        val timesheetTargets: StateFlow<TimesheetTargets> =
            timesheetPreferences.targets.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                TimesheetTargets(),
            )

        val worklogBeingViewed =
            savedStateHandle
                .getStateFlow<Long?>(SAVED_VIEWED_WORKLOG_ID, null)
                .flatMapLatest { worklogId ->
                    if (worklogId == null) {
                        emptyFlow()
                    } else {
                        jiraRepository.flowWorklog(worklogId)
                    }
                }.map { entity -> entity?.let { WorklogIssueUiModel.fromEntity(entity = it) } }

        val pendingWorklogs =
            jiraRepository
                .getPendingWorklogs()
                .map { list ->
                    list.map { WorklogUiModel.fromEntity(entity = it) }
                }

        val pagedPendingWorklogs: Flow<PagingData<WorklogUiModel>> =
            jiraRepository
                .pagedPendingWorklogs()
                .map { pagingData ->
                    pagingData.map { entity -> WorklogUiModel.fromEntity(entity = entity) }
                }
                .cachedIn(viewModelScope)

        val worklogsTodayFromMe =
            jiraRepository
                .getWorklogsTodayFromMe()
                .map { list ->
                    list.map { WorklogIssueUiModel.fromEntity(entity = it) }
                }

        val worklogIssuesLastTwoWeeks =
            jiraRepository
                .getWorklogIssuesInLastTwoWeeks()
                .map { list ->
                    list.map { WorklogIssueUiModel.fromEntity(entity = it) }
                }

        val timesheetProgress: Flow<TimesheetProgress> =
            worklogIssuesLastTwoWeeks.map { list ->
                val now = Instant.now()
                val zone = ZoneId.systemDefault()
                val today = LocalDate.now(zone)
                val (weekStart, weekEnd) = WorklogTimeHelpers.currentWeekWindow(today, zone)
                val todayHours =
                    list
                        .filter { it.from.atZone(zone).toLocalDate() == today }
                        .sumOf { WorklogTimeHelpers.hours(it.from, it.to, it.active, now) }
                val weekHours =
                    list
                        .filter {
                            val to = WorklogTimeHelpers.effectiveTo(it.from, it.to, it.active, now)
                            WorklogTimeHelpers.overlaps(it.from, to, weekStart, weekEnd)
                        }.sumOf { WorklogTimeHelpers.hours(it.from, it.to, it.active, now) }
                TimesheetProgress(todayHours = todayHours, weekHours = weekHours)
            }

        private val _isUpdatingWorklog = MutableStateFlow(false)
        val isUpdatingWorklog: StateFlow<Boolean> = _isUpdatingWorklog.asStateFlow()

        private val _uploadState = MutableStateFlow<WorklogUploadResult>(WorklogUploadResult.Idle)
        val uploadState: StateFlow<WorklogUploadResult> = _uploadState.asStateFlow()

        private val _isRefreshingSummary = MutableStateFlow(false)
        val isRefreshingSummary: StateFlow<Boolean> = _isRefreshingSummary.asStateFlow()

        private val _summaryRefreshError = MutableStateFlow<String?>(null)
        val summaryRefreshError: StateFlow<String?> = _summaryRefreshError.asStateFlow()

        private val _splitError = MutableStateFlow<String?>(null)
        val splitError: StateFlow<String?> = _splitError.asStateFlow()

        fun clearSummaryRefreshError() {
            _summaryRefreshError.value = null
        }

        fun clearSplitError() {
            _splitError.value = null
        }

        fun setViewedWorklogId(id: Long?) {
            savedStateHandle[SAVED_VIEWED_WORKLOG_ID] = id
        }

        fun postPendingWorklogs() {
            viewModelScope.launch {
                _uploadState.value = WorklogUploadResult.Running
                val result = jiraRepository.postPendingWorklogs()
                _uploadState.value = result
                widgetRefresher.refreshAll()
            }
        }

        fun clearUploadState() {
            _uploadState.value = WorklogUploadResult.Idle
        }

        fun updateWorklog(
            worklogId: Long,
            from: Instant,
            to: Instant,
            summary: String,
        ) {
            viewModelScope.launch {
                _isUpdatingWorklog.value = true
                try {
                    jiraRepository.updateWorklog(worklogId, from, to, summary)
                    widgetRefresher.refreshAll()
                } finally {
                    _isUpdatingWorklog.value = false
                }
            }
        }

        suspend fun updateWorklogAwait(
            worklogId: Long,
            from: Instant,
            to: Instant,
            summary: String,
        ) {
            _isUpdatingWorklog.value = true
            try {
                jiraRepository.updateWorklog(worklogId, from, to, summary)
                widgetRefresher.refreshAll()
            } finally {
                _isUpdatingWorklog.value = false
            }
        }

        fun incrementWorklogTime(
            worklogId: Long,
            amount: Long,
        ) {
            viewModelScope.launch {
                jiraRepository.incrementWorklogTime(worklogId, amount)
            }
        }

        fun decrementWorklogTime(
            worklogId: Long,
            amount: Long,
        ) {
            viewModelScope.launch {
                jiraRepository.decrementWorklogTime(worklogId, amount)
            }
        }

        fun moveWorklogIssue(
            worklogId: Long,
            issueId: IssueId,
        ) {
            viewModelScope.launch {
                jiraRepository.moveWorklogIssue(worklogId, issueId)
                widgetRefresher.refreshAll()
            }
        }

        fun splitWorklog(
            worklogId: Long,
            issueId: IssueId,
            splitTime: Instant,
            withNewId: suspend (Long) -> Unit,
        ) {
            viewModelScope.launch {
                try {
                    val success = jiraRepository.splitWorklog(worklogId, issueId, splitTime, withNewId)
                    if (!success) {
                        Timber.e("Split worklog operation returned false for worklogId: $worklogId")
                        _splitError.value = "split_failed"
                    } else {
                        widgetRefresher.refreshAll()
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to split worklog $worklogId to issue ${issueId.value}")
                    _splitError.value = e.message ?: "split_failed"
                }
            }
        }

        /** Count of non-active pending worklogs with blank summaries. */
        fun countBlankSummaries(pending: List<WorklogUiModel>): Int =
            pending.count { !it.active && it.comment.isBlank() }

        fun deleteLog(
            worklogId: Long,
            after: () -> Unit = {},
        ) {
            viewModelScope.launch {
                jiraRepository.deleteWorklog(worklogId)
                after()
            }
        }

        fun stopLogging(
            summary: String,
            after: () -> Unit = {},
        ) {
            viewModelScope.launch {
                jiraRepository.stopLogging(summary)
                widgetRefresher.refreshAll()
                after()
            }
        }

        private val _isStoppingWorklog = MutableStateFlow(false)
        val isStoppingWorklog: StateFlow<Boolean> = _isStoppingWorklog.asStateFlow()

        fun stopLoggingFor(
            worklogId: Long,
            summary: String,
            after: () -> Unit = {},
        ) {
            viewModelScope.launch {
                _isStoppingWorklog.value = true
                try {
                    jiraRepository.stopLoggingFor(worklogId, summary)
                    widgetRefresher.refreshAll()
                    after()
                } finally {
                    _isStoppingWorklog.value = false
                }
            }
        }

        suspend fun stopLoggingForAwait(
            worklogId: Long,
            summary: String,
        ) {
            _isStoppingWorklog.value = true
            try {
                jiraRepository.stopLoggingFor(worklogId, summary)
                widgetRefresher.refreshAll()
            } finally {
                _isStoppingWorklog.value = false
            }
        }

        fun stopLoggingAudioLog(
            worklogId: Long,
            after: (summary: String) -> Unit = {},
        ) {
            viewModelScope.launch {
                _isStoppingWorklog.value = true
                try {
                    val summary = audioRepository.getAudioText()
                    jiraRepository.stopLoggingFor(worklogId, summary)
                    widgetRefresher.refreshAll()
                    after(summary)
                } finally {
                    _isStoppingWorklog.value = false
                }
            }
        }

        /**
         * Stops the given worklog with speech-to-text summary.
         * @return the captured summary (may be blank if speech failed or was empty)
         */
        suspend fun stopLoggingAudioLogAwait(worklogId: Long): String {
            _isStoppingWorklog.value = true
            try {
                val summary = audioRepository.getAudioText()
                jiraRepository.stopLoggingFor(worklogId, summary)
                widgetRefresher.refreshAll()
                return summary
            } finally {
                _isStoppingWorklog.value = false
            }
        }

        fun refreshWorklogsToday(): Job =
            viewModelScope.launch {
                Timber.v("Refreshing worklogs for today from Jira...")
                jiraRepository.refreshWorklogsToday().onFailure { e ->
                    _summaryRefreshError.value = e.message ?: "Refresh failed"
                }
            }

        fun refreshWorklogsLastTwoWeeks(): Job =
            viewModelScope.launch {
                _isRefreshingSummary.value = true
                _summaryRefreshError.value = null
                try {
                    Timber.v("Refreshing worklogs for last two weeks from Jira…")
                    jiraRepository.refreshWorklogsForLastTwoWeeks().onFailure { e ->
                        _summaryRefreshError.value = e.message ?: "Refresh failed"
                    }
                } finally {
                    _isRefreshingSummary.value = false
                }
            }

        companion object {
            private const val SAVED_VIEWED_WORKLOG_ID = "viewedWorklogId"
        }
    }
