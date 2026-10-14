package dae.aevum.ui.composables.worklogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Button
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import dae.aevum.common.R
import dae.aevum.ui.composables.reusable.TimesheetProgressBanner
import dae.aevum.ui.viewmodels.WorklogViewModel
import dae.aevum.utils.TimesheetProgress
import dae.aevum.utils.WorklogTimeHelpers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun WorklogSummaryScreen(
    viewModel: WorklogViewModel = hiltViewModel(),
    onOpenBarChart: () -> Unit,
    onOpenDayDetailsForDay: (LocalDate) -> Unit,
    onBannerClick: (() -> Unit)? = null,
    weekdayTargetHours: Double = WorklogTimeHelpers.WEEKDAY_TARGET_HOURS,
    weekTargetHours: Double = WorklogTimeHelpers.WEEK_TARGET_HOURS,
    workingDays: Set<java.time.DayOfWeek> = WorklogTimeHelpers.DEFAULT_WORKING_DAYS,
) {
    val worklogs by viewModel.worklogIssuesForSummary.collectAsState(initial = emptyList())
    val progress by viewModel.timesheetProgress.collectAsState(initial = TimesheetProgress(0.0, 0.0))
    val isRefreshing by viewModel.isRefreshingSummary.collectAsState()
    val isLoadingMore by viewModel.isLoadingMoreSummary.collectAsState()
    val weekCount by viewModel.summaryWeekCount.collectAsState()
    val refreshError by viewModel.summaryRefreshError.collectAsState()
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val refreshErrorMessage = refreshError?.let { stringResource(it) }

    LaunchedEffect(Unit) {
        viewModel.refreshWorklogsLastTwoWeeks()
    }

    LaunchedEffect(refreshErrorMessage) {
        val message = refreshErrorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearSummaryRefreshError()
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
    val hoursByDay =
        remember(worklogs, nowState.value) {
            val displayWorklogs =
                worklogs.map { e ->
                    if (e.active) {
                        e.copy(to = WorklogTimeHelpers.effectiveTo(e.from, e.to, true, nowState.value))
                    } else {
                        e
                    }
                }
            displayWorklogs
                .groupBy { it.from.atZone(zone).toLocalDate() }
                .mapValues { (_, entries) ->
                    entries.sumOf {
                        WorklogTimeHelpers.hours(it.from, it.to, active = false, now = it.to)
                    }
                }
        }

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
            hoursByDay = hoursByDay,
            weekCount = weekCount,
            isLoadingMore = isLoadingMore,
            canLoadMore = weekCount < WorklogViewModel.MAX_SUMMARY_WEEKS,
            progress = progress,
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshWorklogsLastTwoWeeks() },
            onLoadMore = { viewModel.loadMoreSummaryHistory() },
            onOpenBarChart = onOpenBarChart,
            onOpenDayDetailsForDay = onOpenDayDetailsForDay,
            onBannerClick = onBannerClick,
            weekdayTargetHours = weekdayTargetHours,
            weekTargetHours = weekTargetHours,
            workingDays = workingDays,
        )
        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
        )
        androidx.compose.material3.SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun WorklogSummaryContent(
    hoursByDay: Map<LocalDate, Double>,
    weekCount: Int,
    isLoadingMore: Boolean,
    canLoadMore: Boolean,
    progress: TimesheetProgress,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onOpenBarChart: () -> Unit,
    onOpenDayDetailsForDay: (LocalDate) -> Unit,
    onBannerClick: (() -> Unit)? = null,
    weekdayTargetHours: Double = WorklogTimeHelpers.WEEKDAY_TARGET_HOURS,
    weekTargetHours: Double = WorklogTimeHelpers.WEEK_TARGET_HOURS,
    workingDays: Set<java.time.DayOfWeek> = WorklogTimeHelpers.DEFAULT_WORKING_DAYS,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
    ) {
        TimesheetProgressBanner(
            todayHours = progress.todayHours,
            weekHours = progress.weekHours,
            weekdayTargetHours = weekdayTargetHours,
            weekTargetHours = weekTargetHours,
            workingDays = workingDays,
            onClick = onBannerClick,
        )
        Spacer(modifier = Modifier.height(8.dp))
        /*
        Temporarily removed to test how the system looks without the control bar
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
         */
        Spacer(modifier = Modifier.height(4.dp))
        SummaryHoursLegend()
        Spacer(modifier = Modifier.height(8.dp))
        WorklogSummaryWeekCalendar(
            hoursByDay = hoursByDay,
            weekCount = weekCount,
            isLoadingMore = isLoadingMore,
            canLoadMore = canLoadMore,
            onLoadMore = onLoadMore,
            onOpenDayDetailsForDay = onOpenDayDetailsForDay,
            weekdayTargetHours = weekdayTargetHours,
            workingDays = workingDays,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SummaryHoursLegend() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LegendSwatch(
            color = MaterialTheme.colorScheme.errorContainer,
            label = stringResource(R.string.summary_legend_under),
        )
        LegendSwatch(
            color = MaterialTheme.colorScheme.primaryContainer,
            label = stringResource(R.string.summary_legend_met),
        )
        LegendSwatch(
            color = MaterialTheme.colorScheme.tertiaryContainer,
            label = stringResource(R.string.summary_legend_over),
        )
    }
}

@Composable
private fun LegendSwatch(
    color: Color,
    label: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview
@Composable
private fun WorklogSummaryScreenPreview() {
    val today = LocalDate.now()
    WorklogSummaryContent(
        hoursByDay =
            mapOf(
                today to 5.0,
                today.minusDays(1) to 3.0,
                today.minusDays(2) to 10.0,
            ),
        weekCount = 4,
        isLoadingMore = false,
        canLoadMore = true,
        progress = TimesheetProgress(5.0, 22.0),
        isRefreshing = false,
        onRefresh = {},
        onLoadMore = {},
        onOpenBarChart = {},
        onOpenDayDetailsForDay = {},
    )
}
