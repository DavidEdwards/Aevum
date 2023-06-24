package dae.aevum.ui.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dae.aevum.domain.repositories.JiraRepository
import dae.aevum.domain.repositories.UserRepository
import dae.aevum.ui.models.IssueUiModel
import dae.aevum.ui.models.WorklogUiModel
import dae.aevum.utils.IssueId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Instant
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class IssueViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repository: JiraRepository,
    userRepository: UserRepository
) : ViewModel() {

    private val savedViewedIssueId =
        savedStateHandle.getStateFlow<String?>(SAVED_VIEWED_ISSUE_ID, null).map {
            if (it == null) null
            else IssueId(it)
        }

    val issueBeingViewed = savedViewedIssueId.flatMapLatest { issueId ->
        if (issueId == null) {
            emptyFlow()
        } else {
            repository.flowIssue(issueId)
        }
    }.map { entity ->
        entity.let { IssueUiModel.fromEntity(entity = it) }
    }

    val worklogsForViewedIssue = savedViewedIssueId.flatMapLatest { issueId ->
        if (issueId == null) {
            emptyFlow()
        } else {
            repository.worklogsForIssue(issueId)
        }
    }.map { list ->
        list.map { WorklogUiModel.fromEntity(entity = it) }
    }

    val activeIssue = repository.flowActiveIssue()
    val issues = repository.flowIssues()

    val activeUser = userRepository.flowActiveUser()

    var searchTerm by mutableStateOf("")

    val filteredIssues = issues.combine(snapshotFlow { searchTerm }) { issues, term ->
        issues.filter {
            it.id.value.contains(term, true) ||
                    it.title.contains(term, true)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun setViewedIssueId(id: IssueId?) {
        savedStateHandle[SAVED_VIEWED_ISSUE_ID] = id?.value
    }

    fun refreshIssues() = viewModelScope.launch {
        Timber.v("Refreshing issues…")
        repository.refreshIssuesfromJira()
    }

    fun refreshWorklogsFor(issueId: IssueId) = viewModelScope.launch {
        Timber.v("Refreshing worklogs…")
        repository.refreshWorklogsFor(issueId)
    }

    fun startLogFor(
        issueId: IssueId
    ) {
        viewModelScope.launch {
            repository.startLogFor(issueId)
        }
    }

    fun deleteLog(worklogId: Long) {
        viewModelScope.launch {
            repository.deleteWorklog(worklogId)
        }
    }

    fun updateWorklog(worklogId: Long, from: Instant, to: Instant, summary: String) {
        viewModelScope.launch {
            repository.updateWorklog(worklogId, from, to, summary)
        }
    }

    fun toggleIssuePin(issueId: IssueId) {
        viewModelScope.launch {
            repository.toggleIssuePin(issueId)
        }
    }

    companion object {
        private const val SAVED_VIEWED_ISSUE_ID = "viewedIssueId"
    }

}