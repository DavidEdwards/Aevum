package dae.aevum.ui.composables.issues

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import dae.aevum.database.entities.IssueEntity
import dae.aevum.ui.composables.reusable.HighlightColors
import dae.aevum.ui.composables.reusable.TimesheetProgressBanner
import dae.aevum.ui.models.IssueUiModel
import dae.aevum.ui.viewmodels.IssueViewModel
import dae.aevum.ui.viewmodels.WorklogViewModel
import dae.aevum.utils.IssueId
import kotlinx.coroutines.launch
import timber.log.Timber

private enum class IssuesTab {
    Search,
    Favourites,
}

@Composable
fun IssueListScreen(
    modifier: Modifier = Modifier,
    viewModel: IssueViewModel = hiltViewModel(),
    onNavigateToIssue: (IssueId) -> Unit,
    onTogglePinIssue: (IssueId) -> Unit = {},
    onNavigateToSummary: () -> Unit = {},
) {
    val activeUser by viewModel.activeUser.collectAsState(initial = null)
    if (activeUser == null) {
        NoActiveUser()
        return
    }

    val worklogViewModel: WorklogViewModel = hiltViewModel()

    LaunchedEffect(Unit) {
        Timber.v("Reload issue list screen")
        viewModel.refreshIssues()
        worklogViewModel.refreshWorklogsToday()
    }

    IssueList(
        modifier = modifier,
        viewModel = viewModel,
        worklogViewModel = worklogViewModel,
        onIssueSelected = onNavigateToIssue,
        onTogglePinIssue = onTogglePinIssue,
        onNavigateToSummary = onNavigateToSummary,
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun IssueList(
    modifier: Modifier = Modifier,
    viewModel: IssueViewModel = hiltViewModel(),
    worklogViewModel: WorklogViewModel = hiltViewModel(),
    onIssueSelected: (IssueId) -> Unit,
    onTogglePinIssue: ((IssueId) -> Unit)? = null,
    onNavigateToSummary: () -> Unit = {},
) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val tabs = IssuesTab.entries
    val refreshError by viewModel.refreshError.collectAsState()
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    val refreshFailedMessage = stringResource(R.string.refresh_failed)

    LaunchedEffect(refreshError) {
        if (refreshError != null) {
            snackbarHostState.showSnackbar(refreshFailedMessage)
            viewModel.clearRefreshError()
        }
    }

    Column(
        modifier =
            modifier
                .padding(horizontal = 16.dp),
    ) {
        val progress by worklogViewModel.timesheetProgress.collectAsState(
            initial = dae.aevum.utils.TimesheetProgress(0.0, 0.0),
        )
        val targets by worklogViewModel.timesheetTargets.collectAsState()
        TimesheetProgressBanner(
            todayHours = progress.todayHours,
            weekHours = progress.weekHours,
            compact = true,
            weekdayTargetHours = targets.weekdayTargetHours,
            weekTargetHours = targets.weekTargetHours,
            onClick = onNavigateToSummary,
        )
        PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
            tabs.forEachIndexed { index, tab ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            text =
                                when (tab) {
                                    IssuesTab.Search -> stringResource(R.string.issues_tab_search)
                                    IssuesTab.Favourites ->
                                        stringResource(R.string.issues_tab_favourites)
                                },
                        )
                    },
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box {
            when (tabs[selectedTabIndex]) {
                IssuesTab.Search ->
                    SearchIssuesContent(
                        viewModel = viewModel,
                        worklogViewModel = worklogViewModel,
                        onIssueSelected = onIssueSelected,
                        onTogglePinIssue = onTogglePinIssue,
                    )
                IssuesTab.Favourites ->
                    FavouritesIssuesContent(
                        viewModel = viewModel,
                        worklogViewModel = worklogViewModel,
                        onIssueSelected = onIssueSelected,
                        onTogglePinIssue = onTogglePinIssue,
                    )
            }
            androidx.compose.material3.SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun SearchIssuesContent(
    viewModel: IssueViewModel,
    worklogViewModel: WorklogViewModel,
    onIssueSelected: (IssueId) -> Unit,
    onTogglePinIssue: ((IssueId) -> Unit)?,
) {
    val pagingItems = viewModel.pagedIssues.collectAsLazyPagingItems()

    val refreshScope = rememberCoroutineScope()
    var refreshing by rememberSaveable { mutableStateOf(false) }

    fun triggerRefresh() {
        refreshScope.launch {
            refreshing = true
            viewModel.refreshIssues().join()
            worklogViewModel.refreshWorklogsToday().join()
            pagingItems.refresh()
            refreshing = false
        }
    }

    val refreshState = rememberPullRefreshState(refreshing, ::triggerRefresh)

    Column {
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

        LaunchedEffect(viewModel.searchTerm, items.isEmpty()) {
            viewModel.searchRemoteIfNeeded(items.isEmpty() && viewModel.searchTerm.isNotBlank())
        }

        val remoteResults by viewModel.remoteSearchResults
        val isSearchingRemote by viewModel.isSearchingRemote
        val displayItems =
            if (items.isNotEmpty()) {
                items
            } else {
                remoteResults
            }

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .pullRefresh(refreshState),
        ) {
            when {
                isSearchingRemote -> {
                    Text(
                        text = stringResource(R.string.searching_remote_issues),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                displayItems.isEmpty() -> {
                    Text(
                        text = stringResource(R.string.no_visible_issues),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                else -> {
                    LazyColumn {
                        itemsIndexed(
                            items = displayItems,
                            key = { _, item -> item.id.value },
                        ) { _, item ->
                            IssueListItem(
                                entity = item,
                                highlight = viewModel.searchTerm.takeIf { it.isNotBlank() },
                                onIssueSelected = onIssueSelected,
                                onTogglePinIssue = onTogglePinIssue,
                            )
                        }
                    }
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

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun FavouritesIssuesContent(
    viewModel: IssueViewModel,
    worklogViewModel: WorklogViewModel,
    onIssueSelected: (IssueId) -> Unit,
    onTogglePinIssue: ((IssueId) -> Unit)?,
) {
    val pinnedIssues by viewModel.pinnedIssues.collectAsState()

    val refreshScope = rememberCoroutineScope()
    var refreshing by rememberSaveable { mutableStateOf(false) }

    fun triggerRefresh() {
        refreshScope.launch {
            refreshing = true
            viewModel.refreshIssues().join()
            worklogViewModel.refreshWorklogsToday().join()
            refreshing = false
        }
    }

    val refreshState = rememberPullRefreshState(refreshing, ::triggerRefresh)

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .pullRefresh(refreshState),
    ) {
        if (pinnedIssues.isEmpty()) {
            Text(
                text = stringResource(R.string.no_favourite_issues),
                style = MaterialTheme.typography.bodyLarge,
            )
        } else {
            LazyColumn {
                items(
                    items = pinnedIssues,
                    key = { it.id.value },
                ) { item ->
                    IssueListItem(
                        entity = item,
                        highlight = null,
                        onIssueSelected = onIssueSelected,
                        onTogglePinIssue = onTogglePinIssue,
                    )
                }
            }
        }

        PullRefreshIndicator(
            refreshing = refreshing,
            state = refreshState,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

@Composable
private fun IssueListItem(
    entity: IssueEntity,
    highlight: String?,
    onIssueSelected: (IssueId) -> Unit,
    onTogglePinIssue: ((IssueId) -> Unit)?,
) {
    val uiModel =
        IssueUiModel.fromEntity(
            entity = entity,
            highlight = highlight,
            pinned = entity.pinned,
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
    onNavigateToEditor: (Long) -> Unit = {},
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
                viewModel.startLogFor(it) { closedId ->
                    onNavigateToEditor(closedId)
                }
            },
            stopLogging = stopLogging,
            deleteWorklog = deleteWorklog,
            changeIssueId = changeIssueId,
            splitWorklog = splitWorklog,
            incrementWorklogTime = incrementWorklogTime,
            decrementWorklogTime = decrementWorklogTime,
            onNavigateToEditor = onNavigateToEditor,
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
