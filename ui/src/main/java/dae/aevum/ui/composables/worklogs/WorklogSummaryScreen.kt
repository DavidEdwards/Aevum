package dae.aevum.ui.composables.worklogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import dae.aevum.common.R
import dae.aevum.ui.composables.reusable.TimesheetProgressBanner
import dae.aevum.ui.models.WorklogIssueUiModel
import dae.aevum.ui.viewmodels.WorklogViewModel
import dae.aevum.utils.IssueId
import dae.aevum.utils.TimesheetProgress
import dae.aevum.utils.WorklogTimeHelpers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun WorklogSummaryScreen(
    viewModel: WorklogViewModel = hiltViewModel(),
    onOpenBarChart: () -> Unit,
    onOpenDayDetailsForDay: (LocalDate) -> Unit,
) {
    val worklogs by viewModel.worklogIssuesLastTwoWeeks.collectAsState(initial = emptyList())
    val progress by viewModel.timesheetProgress.collectAsState(initial = TimesheetProgress(0.0, 0.0))
    val isRefreshing by viewModel.isRefreshingSummary.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.refreshWorklogsLastTwoWeeks()
    }

    val hasActive = worklogs.any { it.active }
    val nowState = remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(hasActive) {
        if (hasActive) {
            while (true) {
                nowState.value = Instant.now()
                delay(1_000L)
            }
        } else {
            nowState.value = Instant.now()
        }
    }

    val zone = ZoneId.systemDefault()
    val displayWorklogs =
        worklogs.map { e ->
            if (e.active) e.copy(to = WorklogTimeHelpers.effectiveTo(e.from, e.to, true, nowState.value)) else e
        }
    val grouped = displayWorklogs.groupBy { it.from.atZone(zone).toLocalDate() }

    val pullRefreshState =
        rememberPullRefreshState(
            refreshing = isRefreshing,
            onRefresh = {
                scope.launch { viewModel.refreshWorklogsLastTwoWeeks().join() }
            },
        )

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .pullRefresh(pullRefreshState),
    ) {
        WorklogSummaryContent(
            grouped = grouped,
            progress = progress,
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshWorklogsLastTwoWeeks() },
            onOpenBarChart = onOpenBarChart,
            onOpenDayDetailsForDay = onOpenDayDetailsForDay,
        )
        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

@Composable
private fun WorklogSummaryContent(
    grouped: Map<LocalDate, List<WorklogIssueUiModel>>,
    progress: TimesheetProgress,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onOpenBarChart: () -> Unit,
    onOpenDayDetailsForDay: (LocalDate) -> Unit,
) {
    val orderedDays =
        (0..13)
            .map { LocalDate.now().minusDays(it.toLong()) }
            .toList()

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
    ) {
        TimesheetProgressBanner(
            todayHours = progress.todayHours,
            weekHours = progress.weekHours,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(onClick = onRefresh, enabled = !isRefreshing) {
                Text(text = stringResource(R.string.refresh))
            }
            Button(onClick = onOpenBarChart) {
                Text(text = stringResource(R.string.bar_chart))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp),
        ) {
            items(orderedDays) { day ->
                val entries = grouped[day].orEmpty()
                val hours =
                    entries.sumOf {
                        WorklogTimeHelpers.hours(it.from, it.to, active = false, now = it.to)
                    }
                val target = WorklogTimeHelpers.dayTargetHours(day)
                val meets = WorklogTimeHelpers.meetsDayTarget(hours, day)

                Card(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                ) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable { onOpenDayDetailsForDay(day) }
                                .background(
                                    if (meets) {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    } else {
                                        MaterialTheme.colorScheme.errorContainer
                                    },
                                )
                                .padding(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = day.toString(),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(text = String.format(Locale.US, "%.1f / %.1f h", hours, target))
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun WorklogSummaryScreenPreview() {
    val now = Instant.now()
    val sample =
        listOf(
            WorklogIssueUiModel(
                workId = 1,
                issueId = IssueId("AAPP-1"),
                author = "You",
                from = now.minusSeconds(3 * 3600),
                to = now.minusSeconds(2 * 3600),
                comment = "Setup project",
                pending = false,
                active = false,
                issueTitle = "AAPP-1",
            ),
            WorklogIssueUiModel(
                workId = 2,
                issueId = IssueId("AAPP-2"),
                author = "You",
                from = now.minusSeconds(6 * 3600),
                to = now.minusSeconds(4 * 3600),
                comment = "Implement feature",
                pending = false,
                active = false,
                issueTitle = "AAPP-2",
            ),
        )
    val grouped = mapOf(LocalDate.now() to sample)
    WorklogSummaryContent(
        grouped = grouped,
        progress = TimesheetProgress(5.0, 22.0),
        isRefreshing = false,
        onRefresh = {},
        onOpenBarChart = {},
        onOpenDayDetailsForDay = {},
    )
}
