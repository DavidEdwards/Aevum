package dae.aevum.ui.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dae.aevum.domain.repositories.AudioRepository
import dae.aevum.domain.repositories.JiraRepository
import dae.aevum.ui.models.WorklogUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WorklogViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val jiraRepository: JiraRepository,
    private val audioRepository: AudioRepository
) : ViewModel() {

    val worklogBeingViewed = savedStateHandle.getStateFlow<Long?>(SAVED_VIEWED_WORKLOG_ID, null)
        .flatMapLatest { worklogId ->
            if (worklogId == null) {
                emptyFlow()
            } else {
                jiraRepository.flowWorklog(worklogId)
            }
        }
        .map { entity -> entity?.let { WorklogUiModel.fromEntity(entity = it) } }

    val pendingWorklogs = jiraRepository.getPendingWorklogs()
        .map { list ->
            list.map { WorklogUiModel.fromEntity(entity = it) }
        }

    val worklogsToday = jiraRepository.getWorklogsToday()
        .map { list ->
            list.map { WorklogUiModel.fromEntity(entity = it) }
        }

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
            jiraRepository.updateWorklog(worklogId, from, to, summary)
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
            after()
        }
    }

    fun stopLoggingAudioLog(after: () -> Unit = {}) {
        viewModelScope.launch {
            val summary = audioRepository.getAudioText()
            jiraRepository.stopLogging(summary)
            after()
        }
    }

    companion object {
        private const val SAVED_VIEWED_WORKLOG_ID = "viewedWorklogId"
    }

}