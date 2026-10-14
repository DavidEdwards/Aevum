package dae.aevum.ui.viewmodels

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import dae.aevum.domain.preferences.ProofreaderLogLine
import dae.aevum.domain.preferences.ProofreaderProgress
import dae.aevum.domain.preferences.SummaryHistoryState
import dae.aevum.domain.preferences.TimesheetPreferences
import dae.aevum.domain.preferences.TimesheetTargets
import dae.aevum.domain.repositories.AudioRepository
import dae.aevum.domain.repositories.JiraRepository
import dae.aevum.domain.repositories.WidgetRefresher
import dae.aevum.ui.models.WorklogIssueUiModel
import dae.aevum.ui.models.WorklogUiModel
import dae.aevum.utils.IssueId
import dae.aevum.utils.NetworkErrors
import dae.aevum.utils.TimesheetProgress
import dae.aevum.utils.WorklogTimeHelpers
import dae.aevum.utils.WorklogUploadResult
import androidx.annotation.StringRes
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
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
        private val proofreaderProgress: ProofreaderProgress,
    ) : ViewModel() {
        val proofreaderLogLines: StateFlow<List<ProofreaderLogLine>> = proofreaderProgress.lines

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
                }.cachedIn(viewModelScope)

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

        private val _summaryWeekCount = MutableStateFlow(SummaryHistoryState.DEFAULT_SUMMARY_WEEKS)
        val summaryWeekCount: StateFlow<Int> = _summaryWeekCount.asStateFlow()

        private val _isLoadingMoreSummary = MutableStateFlow(false)
        val isLoadingMoreSummary: StateFlow<Boolean> = _isLoadingMoreSummary.asStateFlow()

        /** Upper bound of the last successful summary remote sync (newer refreshes start here). */
        private var summaryFetchedUntil: Instant? = null

        /** Start of the oldest range already pulled from Jira into the local DB. */
        private var summaryOldestSyncedStart: Instant? = null

        init {
            viewModelScope.launch {
                val history = timesheetPreferences.summaryHistory.first()
                _summaryWeekCount.value = history.weekCount
                summaryFetchedUntil = history.fetchedUntil
                summaryOldestSyncedStart = history.oldestSyncedStart
                Timber.v(
                    "Restored summary history: weeks=%d fetchedUntil=%s oldestSynced=%s",
                    history.weekCount,
                    history.fetchedUntil,
                    history.oldestSyncedStart,
                )
            }
        }

        val worklogIssuesForSummary: Flow<List<WorklogIssueUiModel>> =
            _summaryWeekCount
                .flatMapLatest { weeks ->
                    val (start, end) = WorklogTimeHelpers.summaryWindow(weeks)
                    jiraRepository.getWorklogIssuesInRange(start, end)
                }.map { list ->
                    list.map { WorklogIssueUiModel.fromEntity(entity = it) }
                }

        /**
         * Worklogs overlapping a single local calendar day.
         * Independent of [summaryWeekCount] so day details work after navigating from
         * a summary screen that had loaded older history in a different ViewModel instance.
         */
        fun worklogIssuesForDay(day: LocalDate): Flow<List<WorklogIssueUiModel>> {
            val start = WorklogTimeHelpers.startOfDay(day)
            val end = WorklogTimeHelpers.endOfDayExclusive(day)
            return jiraRepository.getWorklogIssuesInRange(start, end).map { list ->
                list.map { WorklogIssueUiModel.fromEntity(entity = it) }
            }
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

        private val _summaryRefreshError = MutableStateFlow<Int?>(null)
        val summaryRefreshError: StateFlow<Int?> = _summaryRefreshError.asStateFlow()

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
                proofreaderProgress.clear()
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
            proofreaderProgress.clear()
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
        fun countBlankSummaries(pending: List<WorklogUiModel>): Int = pending.count { !it.active && it.comment.isBlank() }

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
                proofreaderProgress.clear()
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
            proofreaderProgress.clear()
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
                proofreaderProgress.clear()
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
            proofreaderProgress.clear()
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
                    reportRefreshError(e)
                }
            }

        fun refreshWorklogsLastTwoWeeks(): Job =
            viewModelScope.launch {
                _isRefreshingSummary.value = true
                _summaryRefreshError.value = null
                try {
                    val (_, rangeEnd) = WorklogTimeHelpers.summaryWindow(_summaryWeekCount.value)
                    val previousUntil = summaryFetchedUntil
                    val rangeStart =
                        if (previousUntil != null) {
                            // Small overlap so late edits near the watermark are still picked up.
                            previousUntil.minus(1, ChronoUnit.DAYS)
                        } else {
                            WorklogTimeHelpers.summaryWindow(SummaryHistoryState.DEFAULT_SUMMARY_WEEKS).first
                        }
                    Timber.v(
                        "Refreshing summary worklogs from Jira since %s (incremental=%s)…",
                        rangeStart,
                        previousUntil != null,
                    )
                    jiraRepository.refreshWorklogsInRange(rangeStart, rangeEnd).onFailure { e ->
                        reportRefreshError(e)
                        return@launch
                    }
                    val now = Instant.now()
                    summaryFetchedUntil = now
                    timesheetPreferences.setSummaryFetchedUntil(now)
                    // First successful sync establishes how far back local history is covered.
                    if (summaryOldestSyncedStart == null || rangeStart.isBefore(summaryOldestSyncedStart)) {
                        summaryOldestSyncedStart = rangeStart
                        timesheetPreferences.setSummaryOldestSyncedStart(rangeStart)
                    }
                } finally {
                    _isRefreshingSummary.value = false
                }
            }

        /**
         * Expand the calendar further into the past. Uses the local DB when that range was
         * already synced; only hits Jira for history older than [summaryOldestSyncedStart].
         */
        fun loadMoreSummaryHistory(): Job =
            viewModelScope.launch {
                if (_isLoadingMoreSummary.value) return@launch
                val current = _summaryWeekCount.value
                if (current >= MAX_SUMMARY_WEEKS) return@launch

                _isLoadingMoreSummary.value = true
                try {
                    val newCount = (current + SUMMARY_WEEKS_PAGE).coerceAtMost(MAX_SUMMARY_WEEKS)
                    val (newStart, _) = WorklogTimeHelpers.summaryWindow(newCount)
                    val (oldStart, _) = WorklogTimeHelpers.summaryWindow(current)
                    val oldestSynced = summaryOldestSyncedStart

                    val needsRemote =
                        oldestSynced == null || newStart.isBefore(oldestSynced)
                    if (needsRemote) {
                        val fetchEnd = oldestSynced ?: oldStart
                        Timber.v(
                            "Fetching older summary history from Jira %s → %s (weeks %d → %d)…",
                            newStart,
                            fetchEnd,
                            current,
                            newCount,
                        )
                        jiraRepository.refreshWorklogsInRange(newStart, fetchEnd).onFailure { e ->
                            reportRefreshError(e)
                            return@launch
                        }
                        summaryOldestSyncedStart = newStart
                        timesheetPreferences.setSummaryOldestSyncedStart(newStart)
                    } else {
                        Timber.v(
                            "Using cached DB history for weeks %d → %d (already synced to %s)",
                            current,
                            newCount,
                            oldestSynced,
                        )
                    }

                    _summaryWeekCount.value = newCount
                    timesheetPreferences.setSummaryWeekCount(newCount)
                } finally {
                    _isLoadingMoreSummary.value = false
                }
            }

        private fun reportRefreshError(error: Throwable) {
            @StringRes val messageRes = NetworkErrors.messageRes(error)
            _summaryRefreshError.value = messageRes
            Timber.w(error, "Worklog refresh failed")
        }

        companion object {
            private const val SAVED_VIEWED_WORKLOG_ID = "viewedWorklogId"
            const val INITIAL_SUMMARY_WEEKS = SummaryHistoryState.DEFAULT_SUMMARY_WEEKS
            const val SUMMARY_WEEKS_PAGE = 4
            const val MAX_SUMMARY_WEEKS = SummaryHistoryState.MAX_SUMMARY_WEEKS
        }
    }
