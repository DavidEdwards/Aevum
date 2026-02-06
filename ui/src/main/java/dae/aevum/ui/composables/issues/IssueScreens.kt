package dae.aevum.ui.composables.issues

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import dae.aevum.common.R
import dae.aevum.ui.composables.reusable.HighlightColors
import dae.aevum.ui.models.IssueUiModel
import dae.aevum.ui.viewmodels.IssueViewModel
import dae.aevum.utils.IssueId
import kotlinx.coroutines.launch
import timber.log.Timber

@Composable
fun IssueListScreen(
    modifier: Modifier = Modifier,
    viewModel: IssueViewModel = hiltViewModel(),
    onNavigateToIssue: (IssueId) -> Unit,
    onTogglePinIssue: (IssueId) -> Unit = {},
) {
    val activeUser by viewModel.activeUser.collectAsState(initial = null)
    if (activeUser == null) {
        NoActiveUser()
        return
    }

    LaunchedEffect(Unit) {
        Timber.v("Reload issue list screen")
        viewModel.refreshIssues()
    }

    IssueList(
        modifier = modifier,
        viewModel = viewModel,
        onIssueSelected = onNavigateToIssue,
        onTogglePinIssue = onTogglePinIssue,
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun IssueList(
    modifier: Modifier = Modifier,
    viewModel: IssueViewModel = hiltViewModel(),
    onIssueSelected: (IssueId) -> Unit,
    onTogglePinIssue: ((IssueId) -> Unit)? = null,
) {
    val pagingItems = viewModel.pagedIssues.collectAsLazyPagingItems()

    val refreshScope = rememberCoroutineScope()
    var refreshing by rememberSaveable { mutableStateOf(false) }

    fun triggerRefresh() {
        refreshScope.launch {
            refreshing = true
            viewModel.refreshIssues().join()
            pagingItems.refresh()
            refreshing = false
        }
    }

    val refreshState = rememberPullRefreshState(refreshing, ::triggerRefresh)

    Column(
        modifier =
            modifier
                .padding(horizontal = 16.dp),
    ) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = viewModel.searchTerm,
            label = {
                Text(text = stringResource(R.string.search_term))
            },
            onValueChange = { term ->
                viewModel.searchTerm = term
            },
        )

        Spacer(modifier = Modifier.height(8.dp))

        val items =
            pagingItems.itemSnapshotList.items.filter { entity ->
                val term = viewModel.searchTerm
                term.isBlank() ||
                    entity.id.value.contains(term, ignoreCase = true) ||
                    entity.title.contains(term, ignoreCase = true)
            }

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .pullRefresh(refreshState),
        ) {
            LazyColumn {
                itemsIndexed(
                    items = items,
                    key = { _, item -> item.id.value },
                ) { _, item ->
                    val uiModel =
                        IssueUiModel.fromEntity(
                            entity = item,
                            highlight = viewModel.searchTerm.takeIf { it.isNotBlank() },
                            pinned = item.pinned,
                            highlightColors =
                                HighlightColors(
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    background = MaterialTheme.colorScheme.primaryContainer,
                                ),
                        )

                    Spacer(modifier = Modifier.height(8.dp))
                    IssueCard(
                        issue = uiModel,
                        onNavigateToIssue = onIssueSelected,
                        onTogglePinIssue = onTogglePinIssue,
                    )
                }
            }

            PullRefreshIndicator(
                refreshing = refreshing,
                state = refreshState,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    }
}

@Composable
fun IssueScreen(
    modifier: Modifier = Modifier,
    viewModel: IssueViewModel = hiltViewModel(),
    issueId: IssueId?,
    stopLogging: (Long) -> Unit = {},
    deleteWorklog: (Long) -> Unit = {},
    changeIssueId: (Long) -> Unit = {},
    splitWorklog: (Long) -> Unit = {},
    incrementWorklogTime: (workId: Long, amount: Long) -> Unit,
    decrementWorklogTime: (workId: Long, amount: Long) -> Unit,
) {
    viewModel.setViewedIssueId(issueId)
    val model by viewModel.issueBeingViewed.collectAsState(initial = null)
    if (model == null) {
        IssueInvalid()
        return
    }

    Column(
        modifier =
            modifier
                .fillMaxSize(),
    ) {
        IssueDetails(
            model = model!!,
            startLogging = {
                viewModel.startLogFor(it)
            },
            stopLogging = stopLogging,
            deleteWorklog = deleteWorklog,
            changeIssueId = changeIssueId,
            splitWorklog = splitWorklog,
            incrementWorklogTime = incrementWorklogTime,
            decrementWorklogTime = decrementWorklogTime,
        )
    }
}

@Composable
fun IssueInvalid() {
    Column(
        modifier =
            Modifier
                .padding(16.dp)
                .fillMaxSize(),
    ) {
        Text(
            text = stringResource(R.string.issue_not_available),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
fun NoActiveUser() {
    Column(
        modifier =
            Modifier
                .padding(16.dp)
                .fillMaxSize(),
    ) {
        Text(
            text = stringResource(R.string.no_active_user_error),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
