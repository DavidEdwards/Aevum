package dae.aevum.ui.composables.worklogs

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import dae.aevum.common.R
import dae.aevum.ui.viewmodels.WorklogViewModel
import dae.aevum.utils.WorklogTimeHelpers
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun WorklogBarChartScreen(
    viewModel: WorklogViewModel = hiltViewModel(),
    weekdayTargetHours: Double = WorklogTimeHelpers.WEEKDAY_TARGET_HOURS,
) {
    val worklogs by viewModel.worklogIssuesLastTwoWeeks.collectAsState(initial = emptyList())
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
            if (e.active) {
                e.copy(to = WorklogTimeHelpers.effectiveTo(e.from, e.to, true, nowState.value))
            } else {
                e
            }
        }
    val grouped = displayWorklogs.groupBy { it.from.atZone(zone).toLocalDate() }
    val orderedDays = (0..13).map { LocalDate.now().minusDays(it.toLong()) }
    val values =
        orderedDays.map { day ->
            val entries = grouped[day].orEmpty()
            entries
                .sumOf {
                    WorklogTimeHelpers.hours(it.from, it.to, active = false, now = it.to)
                }.toFloat()
        }

    val goodColor = MaterialTheme.colorScheme.primary
    val badColor = MaterialTheme.colorScheme.error
    val lineColor = Color.Gray
    val weekdayTargetLine = weekdayTargetHours.toFloat()

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(
            text = stringResource(R.string.last_14_days),
            style = MaterialTheme.typography.titleMedium,
        )
        Canvas(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(top = 16.dp),
        ) {
            val barCount = values.size
            if (barCount == 0) return@Canvas
            val widthPerBar = size.width / (barCount * 1.5f)
            val gap = widthPerBar / 2f
            val maxHours = (values.maxOrNull() ?: weekdayTargetLine).coerceAtLeast(weekdayTargetLine)

            values.forEachIndexed { index, hours ->
                val day = orderedDays[index]
                val barHeight = (hours / maxHours) * size.height * 0.8f
                val x = gap + index * (widthPerBar + gap)
                val y = size.height - barHeight
                drawRect(
                    color =
                        if (WorklogTimeHelpers.isDayOnTrack(hours, day, weekdayTargetHours)) {
                            goodColor
                        } else {
                            badColor
                        },
                    topLeft = Offset(x, y),
                    size = Size(widthPerBar, barHeight),
                )
            }

            // Draw weekday target line as visual reference
            val targetY = size.height - (weekdayTargetLine / maxHours) * size.height * 0.8f
            drawLine(
                color = lineColor,
                start = Offset(0f, targetY),
                end = Offset(size.width, targetY),
                strokeWidth = 2f,
            )
        }
    }
}

@Preview
@Composable
private fun WorklogBarChartScreenPreview() {
    WorklogBarChartScreen()
}
