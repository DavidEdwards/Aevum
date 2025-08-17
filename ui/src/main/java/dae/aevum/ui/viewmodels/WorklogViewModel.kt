package dae.aevum.ui.viewmodels

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dae.aevum.domain.repositories.AudioRepository
import dae.aevum.domain.repositories.JiraRepository
import dae.aevum.domain.repositories.WidgetRefresher
import dae.aevum.ui.models.WorklogIssueUiModel
import dae.aevum.ui.models.WorklogUiModel
import dae.aevum.utils.IssueId
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Instant
import javax.inject.Inject

// removed unused imports

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WorklogViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val jiraRepository: JiraRepository,
    private val audioRepository: AudioRepository,
    @ApplicationContext private val appContext: Context,
    private val widgetRefresher: WidgetRefresher
) : ViewModel() {

    val worklogBeingViewed = savedStateHandle.getStateFlow<Long?>(SAVED_VIEWED_WORKLOG_ID, null)
        .flatMapLatest { worklogId ->
            if (worklogId == null) {
                emptyFlow()
            } else {
                jiraRepository.flowWorklog(worklogId)
            }
        }
        .map { entity -> entity?.let { WorklogIssueUiModel.fromEntity(entity = it) } }

    val pendingWorklogs = jiraRepository.getPendingWorklogs()
        .map { list ->
            list.map { WorklogUiModel.fromEntity(entity = it) }
        }

    val worklogsTodayFromMe = jiraRepository.getWorklogsTodayFromMe()
        .map { list ->
            list.map { WorklogIssueUiModel.fromEntity(entity = it) }
        }

    val worklogIssuesLastTwoWeeks = jiraRepository.getWorklogIssuesInLastTwoWeeks()
        .map { list ->
            list.map { WorklogIssueUiModel.fromEntity(entity = it) }
        }

    private val _isUpdatingWorklog = MutableStateFlow(false)
    val isUpdatingWorklog: StateFlow<Boolean> = _isUpdatingWorklog.asStateFlow()

    fun setViewedWorklogId(id: Long?) {
        savedStateHandle[SAVED_VIEWED_WORKLOG_ID] = id
    }

    fun postPendingWorklogs() {
        viewModelScope.launch {
            jiraRepository.postPendingWorklogs()
        }
    }

    fun updateWorklog(worklogId: Long, from: Instant, to: Instant, summary: String) {
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

    suspend fun updateWorklogAwait(worklogId: Long, from: Instant, to: Instant, summary: String) {
        _isUpdatingWorklog.value = true
        try {
            // Call directly; repository already does validation and proofreading
            jiraRepository.updateWorklog(worklogId, from, to, summary)
            widgetRefresher.refreshAll()
        } finally {
            _isUpdatingWorklog.value = false
        }
    }

    fun incrementWorklogTime(worklogId: Long, amount: Long) {
        viewModelScope.launch {
            jiraRepository.incrementWorklogTime(worklogId, amount)
        }
    }

    fun decrementWorklogTime(worklogId: Long, amount: Long) {
        viewModelScope.launch {
            jiraRepository.decrementWorklogTime(worklogId, amount)
        }
    }

    fun moveWorklogIssue(worklogId: Long, issueId: IssueId) {
        viewModelScope.launch {
            jiraRepository.moveWorklogIssue(worklogId, issueId)
            widgetRefresher.refreshAll()
        }
    }

    fun splitWorklog(
        worklogId: Long,
        issueId: IssueId,
        splitTime: Instant,
        withNewId: suspend (Long) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val success = jiraRepository.splitWorklog(worklogId, issueId, splitTime, withNewId)
                if (!success) {
                    Timber.e("Split worklog operation returned false for worklogId: $worklogId")
                } else {
                    widgetRefresher.refreshAll()
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to split worklog $worklogId to issue ${issueId.value}")
            }
        }
    }

    fun deleteLog(worklogId: Long, after: () -> Unit = {}) {
        viewModelScope.launch {
            jiraRepository.deleteWorklog(worklogId)
            after()
        }
    }

    fun stopLogging(summary: String, after: () -> Unit = {}) {
        viewModelScope.launch {
            jiraRepository.stopLogging(summary)
            widgetRefresher.refreshAll()
            after()
        }
    }

    private val _isStoppingWorklog = MutableStateFlow(false)
    val isStoppingWorklog: StateFlow<Boolean> = _isStoppingWorklog.asStateFlow()

    fun stopLoggingFor(worklogId: Long, summary: String, after: () -> Unit = {}) {
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

    suspend fun stopLoggingForAwait(worklogId: Long, summary: String) {
        _isStoppingWorklog.value = true
        try {
            jiraRepository.stopLoggingFor(worklogId, summary)
            widgetRefresher.refreshAll()
        } finally {
            _isStoppingWorklog.value = false
        }
    }

    fun stopLoggingAudioLog(after: () -> Unit = {}) {
        viewModelScope.launch {
            _isStoppingWorklog.value = true
            try {
                val summary = audioRepository.getAudioText()
                jiraRepository.stopLogging(summary)
                widgetRefresher.refreshAll()
                after()
            } finally {
                _isStoppingWorklog.value = false
            }
        }
    }

    suspend fun stopLoggingAudioLogAwait() {
        _isStoppingWorklog.value = true
        try {
            val summary = audioRepository.getAudioText()
            jiraRepository.stopLogging(summary)
            widgetRefresher.refreshAll()
        } finally {
            _isStoppingWorklog.value = false
        }
    }

    fun refreshWorklogsToday(): Job {
        return viewModelScope.launch {
            Timber.v("Refreshing worklogs for today from Jira...")
            jiraRepository.refreshWorklogsToday()
        }
    }

    fun refreshWorklogsLastTwoWeeks(): Job {
        return viewModelScope.launch {
            Timber.v("Refreshing worklogs for last two weeks from Jira…")
            jiraRepository.refreshWorklogsForLastTwoWeeks()
        }
    }

    companion object {
        private const val SAVED_VIEWED_WORKLOG_ID = "viewedWorklogId"
    }

}