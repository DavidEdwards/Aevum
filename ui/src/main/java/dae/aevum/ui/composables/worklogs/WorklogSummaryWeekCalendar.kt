package dae.aevum.ui.composables.worklogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dae.aevum.common.R
import dae.aevum.ui.composables.reusable.listBottomContentPadding
import dae.aevum.utils.SummaryCalendar
import dae.aevum.utils.SummaryCalendarItem
import dae.aevum.utils.WorklogTimeHelpers
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.LocalDate
import java.util.Locale

@Composable
fun WorklogSummaryWeekCalendar(
    hoursByDay: Map<LocalDate, Double>,
    weekCount: Int,
    isLoadingMore: Boolean,
    canLoadMore: Boolean,
    onLoadMore: () -> Unit,
    onOpenDayDetailsForDay: (LocalDate) -> Unit,
    weekdayTargetHours: Double = WorklogTimeHelpers.WEEKDAY_TARGET_HOURS,
    workingDays: Set<java.time.DayOfWeek> = WorklogTimeHelpers.DEFAULT_WORKING_DAYS,
    today: LocalDate = LocalDate.now(),
    modifier: Modifier = Modifier,
) {
    val items =
        remember(weekCount, today) {
            SummaryCalendar.buildItems(weekCount = weekCount, today = today)
        }
    val listState = rememberLazyListState()
    val weekdayLabels = stringArrayResource(R.array.weekday_short_mon_sun)
    // Prevent cascading loads: only fetch the next page after the user scrolls away
    // from the end and back (initial layout often has "near end" == true).
    var loadMoreArmed by remember { mutableStateOf(true) }

    LaunchedEffect(listState, canLoadMore, isLoadingMore) {
        snapshotFlow {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            val nearEnd = lastVisible >= info.totalItemsCount - 2
            val hasScrolled = listState.canScrollBackward
            Triple(nearEnd, hasScrolled, isLoadingMore)
        }.distinctUntilChanged()
            .collect { (nearEnd, hasScrolled, loading) ->
                if (!nearEnd) {
                    loadMoreArmed = true
                    return@collect
                }
                if (loadMoreArmed && hasScrolled && canLoadMore && !loading) {
                    loadMoreArmed = false
                    onLoadMore()
                }
            }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = listBottomContentPadding()),
    ) {
        item(key = "weekday_header") {
            WeekdayHeaderRow(labels = weekdayLabels)
            Spacer(modifier = Modifier.height(6.dp))
        }

        items(
            items = items,
            key = { item ->
                when (item) {
                    is SummaryCalendarItem.MonthHeader -> "m-${item.yearMonth}"
                    is SummaryCalendarItem.WeekRow -> "w-${item.weekStart}"
                }
            },
        ) { item ->
            when (item) {
                is SummaryCalendarItem.MonthHeader -> {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 12.dp, bottom = 6.dp),
                    )
                }

                is SummaryCalendarItem.WeekRow -> {
                    WeekRowCells(
                        days = item.days,
                        hoursByDay = hoursByDay,
                        today = today,
                        weekdayTargetHours = weekdayTargetHours,
                        workingDays = workingDays,
                        onOpenDayDetailsForDay = onOpenDayDetailsForDay,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }

        if (isLoadingMore) {
            item(key = "loading_more") {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.height(28.dp))
                }
            }
        } else if (!canLoadMore) {
            item(key = "end") {
                Text(
                    text = stringResource(R.string.summary_calendar_end),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun WeekdayHeaderRow(labels: Array<String>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        labels.forEach { label ->
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun WeekRowCells(
    days: List<LocalDate>,
    hoursByDay: Map<LocalDate, Double>,
    today: LocalDate,
    weekdayTargetHours: Double,
    workingDays: Set<java.time.DayOfWeek>,
    onOpenDayDetailsForDay: (LocalDate) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        days.forEach { day ->
            val hours = hoursByDay[day] ?: 0.0
            DayHoursCell(
                day = day,
                hours = hours,
                today = today,
                weekdayTargetHours = weekdayTargetHours,
                workingDays = workingDays,
                onClick = { onOpenDayDetailsForDay(day) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun DayHoursCell(
    day: LocalDate,
    hours: Double,
    today: LocalDate,
    weekdayTargetHours: Double,
    workingDays: Set<java.time.DayOfWeek>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val status =
        WorklogTimeHelpers.dayHoursStatus(
            hours = hours,
            day = day,
            today = today,
            weekdayTargetHours = weekdayTargetHours,
            workingDays = workingDays,
        )
    val target = WorklogTimeHelpers.dayTargetHours(day, weekdayTargetHours, workingDays)
    val colors = dayCellColors(status)
    val isToday = day == today
    val clickable = !day.isAfter(today)

    Column(
        modifier =
            modifier
                .aspectRatio(0.72f)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.background)
                .then(
                    if (isToday) {
                        Modifier.border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(10.dp),
                        )
                    } else {
                        Modifier
                    },
                ).clickable(enabled = clickable, onClick = onClick)
                .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = day.dayOfMonth.toString(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
            color = colors.content,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text =
                when (status) {
                    WorklogTimeHelpers.DayHoursStatus.FUTURE -> "—"
                    WorklogTimeHelpers.DayHoursStatus.EMPTY_WEEKEND -> "0"
                    else -> String.format(Locale.US, "%.1f", hours)
                },
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = colors.content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.weight(1f))
        DayHoursMeter(
            hours = hours,
            target = target,
            status = status,
            fillColor = colors.meter,
            trackColor = colors.meterTrack,
        )
    }
}

@Composable
private fun DayHoursMeter(
    hours: Double,
    target: Double,
    status: WorklogTimeHelpers.DayHoursStatus,
    fillColor: Color,
    trackColor: Color,
) {
    if (status == WorklogTimeHelpers.DayHoursStatus.FUTURE) {
        Spacer(modifier = Modifier.height(4.dp))
        return
    }
    // Cap at the day target (8h weekdays); overtime is not shown on the bar.
    val fraction =
        when {
            target <= 0.0 -> 0f
            else -> (hours / target).toFloat().coerceIn(0f, 1f)
        }
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(trackColor),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .height(4.dp)
                    .background(fillColor),
        )
    }
}

private data class DayCellColors(
    val background: Color,
    val content: Color,
    val meter: Color,
    val meterTrack: Color,
)

@Composable
private fun dayCellColors(status: WorklogTimeHelpers.DayHoursStatus): DayCellColors {
    val scheme = MaterialTheme.colorScheme
    return when (status) {
        WorklogTimeHelpers.DayHoursStatus.FUTURE -> {
            DayCellColors(
                background = scheme.surfaceVariant.copy(alpha = 0.35f),
                content = scheme.onSurfaceVariant.copy(alpha = 0.45f),
                meter = Color.Transparent,
                meterTrack = Color.Transparent,
            )
        }

        WorklogTimeHelpers.DayHoursStatus.EMPTY_WEEKEND -> {
            DayCellColors(
                background = scheme.surfaceVariant.copy(alpha = 0.55f),
                content = scheme.onSurfaceVariant.copy(alpha = 0.7f),
                meter = scheme.outlineVariant,
                meterTrack = scheme.outlineVariant.copy(alpha = 0.4f),
            )
        }

        WorklogTimeHelpers.DayHoursStatus.UNDER -> {
            DayCellColors(
                background = scheme.errorContainer,
                content = scheme.onErrorContainer,
                meter = scheme.error,
                meterTrack = scheme.error.copy(alpha = 0.25f),
            )
        }

        WorklogTimeHelpers.DayHoursStatus.MET -> {
            DayCellColors(
                background = scheme.primaryContainer,
                content = scheme.onPrimaryContainer,
                meter = scheme.primary,
                meterTrack = scheme.primary.copy(alpha = 0.25f),
            )
        }

        WorklogTimeHelpers.DayHoursStatus.OVER -> {
            DayCellColors(
                background = scheme.tertiaryContainer,
                content = scheme.onTertiaryContainer,
                meter = scheme.tertiary,
                meterTrack = scheme.tertiary.copy(alpha = 0.25f),
            )
        }
    }
}

@Preview(widthDp = 360, showBackground = true)
@Composable
private fun WorklogSummaryWeekCalendarPreview() {
    val today = LocalDate.of(2026, 4, 8)
    val hours =
        mapOf(
            today to 7.5,
            today.minusDays(1) to 3.0,
            today.minusDays(2) to 10.5,
            today.minusDays(3) to 8.0,
        )
    WorklogSummaryWeekCalendar(
        hoursByDay = hours,
        weekCount = 4,
        isLoadingMore = false,
        canLoadMore = true,
        onLoadMore = {},
        onOpenDayDetailsForDay = {},
        today = today,
    )
}
