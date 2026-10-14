package dae.aevum.ui.composables.worklogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import dae.aevum.common.R
import dae.aevum.ui.composables.issues.IssueList
import dae.aevum.ui.composables.reusable.listBottomContentPadding
import dae.aevum.ui.viewmodels.WorklogViewModel
import dae.aevum.utils.Formatters
import dae.aevum.utils.IssueId
import dae.aevum.utils.human
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.math.max

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun WorklogListScreen(
    modifier: Modifier = Modifier,
    viewModel: WorklogViewModel = hiltViewModel(),
    onNavigateToEditor: (Long) -> Unit,
    stopLogging: (Long) -> Unit = {},
    deleteWorklog: (Long) -> Unit = {},
    changeIssueId: (Long) -> Unit = {},
    splitWorklog: (Long) -> Unit = {},
    incrementWorklogTime: (workId: Long, amount: Long) -> Unit,
    decrementWorklogTime: (workId: Long, amount: Long) -> Unit,
) {
    val pendingWorklogs by viewModel.pendingWorklogs.collectAsState(initial = emptyList())
    val worklogsToday by viewModel.worklogsTodayFromMe.collectAsState(initial = emptyList())
    val refreshError by viewModel.summaryRefreshError.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val refreshErrorMessage = refreshError?.let { stringResource(it) }

    LaunchedEffect(refreshErrorMessage) {
        val message = refreshErrorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearSummaryRefreshError()
    }

    val activeWorklog = pendingWorklogs.firstOrNull { it.active }
    val readyToUpload = pendingWorklogs.filter { !it.active }
    val hasPending = pendingWorklogs.isNotEmpty()

    val postedTotal =
        remember(worklogsToday) {
            worklogsToday
                .fold(Duration.ZERO) { acc, model ->
                    acc.plus(Duration.between(model.from, model.to))
                }.human()
        }

    val refreshScope = rememberCoroutineScope()
    var refreshing by rememberSaveable { mutableStateOf(false) }

    fun triggerRefresh() {
        refreshScope.launch {
            refreshing = true
            try {
                viewModel.refreshWorklogsToday().join()
            } finally {
                refreshing = false
            }
        }
    }

    val refreshState = rememberPullRefreshState(refreshing, ::triggerRefresh)

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .pullRefresh(refreshState),
    ) {
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = listBottomContentPadding()),
        ) {
            if (activeWorklog != null) {
                item(key = "section_active") {
                    PendingSectionHeader(title = stringResource(R.string.currently_timing))
                }
                item(key = "active_${activeWorklog.workId}") {
                    Spacer(modifier = Modifier.height(8.dp))
                    WorklogCard(
                        worklogUiModel = activeWorklog,
                        onNavigate = { onNavigateToEditor(it.workId) },
                        stopLogging = stopLogging,
                        deleteWorklog = deleteWorklog,
                        changeIssueId = changeIssueId,
                        splitWorklog = splitWorklog,
                        incrementWorklogTime = incrementWorklogTime,
                        decrementWorklogTime = decrementWorklogTime,
                    )
                }
            }

            if (hasPending) {
                item(key = "pending_chart") {
                    Spacer(modifier = Modifier.height(12.dp))
                    PendingBreakdownChart(pending = pendingWorklogs)
                }
            }

            if (readyToUpload.isNotEmpty()) {
                item(key = "section_ready") {
                    Spacer(modifier = Modifier.height(8.dp))
                    PendingSectionHeader(title = stringResource(R.string.ready_to_upload))
                }
                itemsIndexed(
                    items = readyToUpload,
                    key = { _, item -> "pending_${item.workId}" },
                ) { _, model ->
                    Spacer(modifier = Modifier.height(8.dp))
                    WorklogCard(
                        worklogUiModel = model,
                        onNavigate = { onNavigateToEditor(it.workId) },
                        stopLogging = stopLogging,
                        deleteWorklog = deleteWorklog,
                        changeIssueId = changeIssueId,
                        splitWorklog = splitWorklog,
                        incrementWorklogTime = incrementWorklogTime,
                        decrementWorklogTime = decrementWorklogTime,
                    )
                }
            } else if (!hasPending) {
                item(key = "empty_pending") {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.no_pending_worklogs),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (worklogsToday.isNotEmpty()) {
                item(key = "section_posted") {
                    Spacer(modifier = Modifier.height(16.dp))
                    PendingSectionHeader(
                        title = stringResource(R.string.worklogs_posted_today_already),
                        trailing = stringResource(R.string.posted_today_total, postedTotal),
                    )
                }
                itemsIndexed(
                    items = worklogsToday,
                    key = { _, item -> "posted_${item.workId}" },
                ) { _, model ->
                    Spacer(modifier = Modifier.height(8.dp))
                    PostedWorklogCard(model = model)
                }
            }
        }

        PullRefreshIndicator(
            refreshing = refreshing,
            state = refreshState,
            modifier = Modifier.align(Alignment.TopCenter),
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
fun WorklogEditorScreen(
    modifier: Modifier = Modifier,
    viewModel: WorklogViewModel = hiltViewModel(),
    worklogId: Long?,
    updateWorklog: (Long, Instant, Instant, String) -> Unit = { _, _, _, _ -> },
    splitWorklog: (Long) -> Unit = {},
) {
    viewModel.setViewedWorklogId(worklogId)
    val model by viewModel.worklogBeingViewed.collectAsState(initial = null)
    if (model == null) {
        WorklogInvalid()
        return
    }

    Column(
        modifier =
            modifier
                .padding(16.dp)
                .fillMaxSize(),
    ) {
        WorklogDetails(
            modifier = Modifier,
            model = model!!,
            updateWorklog = updateWorklog,
            splitWorklog = splitWorklog,
        )
    }
}

@Composable
fun WorklogSplitterIssueScreen(
    modifier: Modifier = Modifier,
    viewModel: WorklogViewModel = hiltViewModel(),
    worklogId: Long?,
    issueSelected: (IssueId) -> Unit = {},
) {
    viewModel.setViewedWorklogId(worklogId)
    val model by viewModel.worklogBeingViewed.collectAsState(initial = null)
    if (model == null) {
        WorklogInvalid()
        return
    }

    IssueList(
        modifier = modifier,
        onIssueSelected = { issueId ->
            issueSelected(issueId)
        },
    )
}

@Composable
fun WorklogSplitterTimeScreen(
    modifier: Modifier = Modifier,
    viewModel: WorklogViewModel = hiltViewModel(),
    worklogId: Long,
    issueId: IssueId,
    splitWorklog: (Long, IssueId, Instant) -> Unit = { _, _, _ -> },
) {
    viewModel.setViewedWorklogId(worklogId)
    val model by viewModel.worklogBeingViewed.collectAsState(initial = null)
    if (model == null) {
        WorklogInvalid()
        return
    }

    val originalIssueId = model!!.issueId
    val start = model!!.from
    val end = model!!.to

    val duration = Duration.between(start, end)
    val totalMinutes = max(duration.toMinutes(), 1L)

    var position: Float by remember {
        mutableFloatStateOf(totalMinutes.toFloat())
    }

    val startLabel = Formatters.universalTimeFormatter.format(start)
    var middleLabel: String by remember {
        mutableStateOf("00:00")
    }
    var endLabel: String by remember {
        mutableStateOf("00:00")
    }

    fun getMiddle(): Instant = Instant.from(start).plus(position.toLong(), ChronoUnit.MINUTES)

    fun recalculateWithPosition() {
        val middle = getMiddle()

        val startToMiddle = Duration.between(start, middle)
        middleLabel = startToMiddle.human()

        val middleToEnd = Duration.between(middle, end)
        endLabel = middleToEnd.human()
    }
    recalculateWithPosition()

    Column(
        modifier =
            modifier
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(state = rememberScrollState()),
    ) {
        Text(
            text =
                stringResource(
                    R.string.splitting_from_to_issue,
                    originalIssueId.value,
                    issueId.value,
                ),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = stringResource(R.string.choose_time))
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier =
                Modifier
                    .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = stringResource(R.string.time_start, startLabel))
            Text(text = stringResource(R.string.time_old_duration, middleLabel))
            Text(text = stringResource(R.string.time_new_duration, endLabel))
        }
        Slider(
            modifier = Modifier.fillMaxWidth(),
            value = position,
            onValueChange = {
                position = it
                recalculateWithPosition()
            },
            valueRange = 1f..totalMinutes.toFloat(),
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            modifier = modifier,
            onClick = {
                splitWorklog(worklogId, issueId, getMiddle())
            },
        ) {
            Icon(
                imageVector = Icons.Default.Done,
                contentDescription = stringResource(R.string.save),
            )
            Text(text = stringResource(R.string.save))
        }

        Spacer(modifier = Modifier.height(listBottomContentPadding()))
    }
}

@Composable
fun WorklogInvalid() {
    Column(
        modifier =
            Modifier
                .padding(16.dp)
                .fillMaxSize(),
    ) {
        Text(
            text = stringResource(R.string.worklog_not_available),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
