package dae.aevum.ui.composables.worklogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dae.aevum.ui.composables.reusable.listBottomContentPadding
import dae.aevum.ui.models.WorklogIssueUiModel
import dae.aevum.utils.WorklogTimeHelpers
import java.time.LocalDate
import java.util.Locale

/**
 * Previous summary day list (14 flat day cards). Kept for reference / rollback;
 * the summary screen now uses [WorklogSummaryWeekCalendar].
 */
@Composable
fun LegacyWorklogSummaryDayList(
    grouped: Map<LocalDate, List<WorklogIssueUiModel>>,
    onOpenDayDetailsForDay: (LocalDate) -> Unit,
    weekdayTargetHours: Double = WorklogTimeHelpers.WEEKDAY_TARGET_HOURS,
    workingDays: Set<java.time.DayOfWeek> = WorklogTimeHelpers.DEFAULT_WORKING_DAYS,
    modifier: Modifier = Modifier,
) {
    val orderedDays =
        (0..13)
            .map { LocalDate.now().minusDays(it.toLong()) }
            .toList()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = listBottomContentPadding()),
    ) {
        items(orderedDays) { day ->
            val entries = grouped[day].orEmpty()
            val hours =
                entries.sumOf {
                    WorklogTimeHelpers.hours(it.from, it.to, active = false, now = it.to)
                }
            val target = WorklogTimeHelpers.dayTargetHours(day, weekdayTargetHours, workingDays)
            val meets = WorklogTimeHelpers.meetsDayTarget(hours, day, weekdayTargetHours, workingDays)

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
                            ).padding(12.dp),
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
