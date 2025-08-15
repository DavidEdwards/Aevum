package dae.aevum.ui.composables.worklogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dae.aevum.ui.models.WorklogIssueUiModel
import dae.aevum.ui.viewmodels.WorklogViewModel
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun WorklogSummaryScreen(
    viewModel: WorklogViewModel = hiltViewModel(),
    onOpenBarChart: () -> Unit,
    onOpenDayDetailsForDay: (LocalDate) -> Unit
) {
    val worklogs by viewModel.worklogIssuesLastTwoWeeks.collectAsState(initial = emptyList())

    LaunchedEffect(Unit) {
        viewModel.refreshWorklogsLastTwoWeeks()
    }

    val grouped = worklogs.groupBy { it.from.atZone(ZoneId.systemDefault()).toLocalDate() }
    WorklogSummaryContent(
        grouped = grouped,
        onRefresh = { viewModel.refreshWorklogsLastTwoWeeks() },
        onOpenBarChart = onOpenBarChart,
        onOpenDayDetailsForDay = onOpenDayDetailsForDay
    )
}

@Composable
private fun WorklogSummaryContent(
    grouped: Map<LocalDate, List<WorklogIssueUiModel>>,
    onRefresh: () -> Unit,
    onOpenBarChart: () -> Unit,
    onOpenDayDetailsForDay: (LocalDate) -> Unit
) {
    val orderedDays = (0..13)
        .map { LocalDate.now().minusDays(it.toLong()) }
        .toList()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onRefresh) {
                Text(text = "Refresh")
            }
            Button(onClick = onOpenBarChart) {
                Text(text = "Bar chart")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 96.dp)
        ) {
            items(orderedDays) { day ->
                val entries = grouped[day].orEmpty()
                val totalSeconds = entries.sumOf { Duration.between(it.from, it.to).seconds }
                val hours = totalSeconds / 3600.0
                val target = if (day.dayOfWeek.value in 1..5) 8.0 else 0.0
                val meets = hours >= target

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenDayDetailsForDay(day) }
                            .background(if (meets) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.errorContainer)
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = day.toString(),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(text = String.format("%.1f / %.1f h", hours, target))
                        }
                        // Keep summary concise: no per-worklog entries here
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun WorklogSummaryScreenPreview() {
    val now = java.time.Instant.now()
    val sample = listOf(
        WorklogIssueUiModel(
            workId = 1,
            issueId = dae.aevum.utils.IssueId("AAPP-1"),
            author = "You",
            from = now.minusSeconds(3 * 3600),
            to = now.minusSeconds(2 * 3600),
            comment = "Setup project",
            pending = false,
            active = false,
            issueTitle = "AAPP-1"
        ),
        WorklogIssueUiModel(
            workId = 2,
            issueId = dae.aevum.utils.IssueId("AAPP-2"),
            author = "You",
            from = now.minusSeconds(6 * 3600),
            to = now.minusSeconds(4 * 3600),
            comment = "Implement feature",
            pending = false,
            active = false,
            issueTitle = "AAPP-2"
        )
    )
    val grouped = mapOf(LocalDate.now() to sample)
    WorklogSummaryContent(
        grouped = grouped,
        onRefresh = {},
        onOpenBarChart = {},
        onOpenDayDetailsForDay = {}
    )
}
