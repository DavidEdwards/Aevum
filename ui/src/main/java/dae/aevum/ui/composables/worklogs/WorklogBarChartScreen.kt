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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import dae.aevum.common.R
import dae.aevum.ui.viewmodels.WorklogViewModel
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun WorklogBarChartScreen(viewModel: WorklogViewModel = hiltViewModel()) {
    val worklogs by viewModel.worklogIssuesLastTwoWeeks.collectAsState(initial = emptyList())
    val grouped = worklogs.groupBy { it.from.atZone(ZoneId.systemDefault()).toLocalDate() }
    val orderedDays = (0..13).map { LocalDate.now().minusDays(it.toLong()) }
    val values =
        orderedDays.map { day ->
            val entries = grouped[day].orEmpty()
            val seconds = entries.sumOf { Duration.between(it.from, it.to).seconds }
            seconds / 3600f
        }

    val goodColor = MaterialTheme.colorScheme.primary
    val badColor = MaterialTheme.colorScheme.error
    val lineColor = Color.Gray

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(
            text = stringResource(R.string.last_14_days),
            style = MaterialTheme.typography.titleMedium
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
            val maxHours = (values.maxOrNull() ?: 8f).coerceAtLeast(8f)
            val target = 8f

            values.forEachIndexed { index, hours ->
                val barHeight = (hours / maxHours) * size.height * 0.8f
                val x = gap + index * (widthPerBar + gap)
                val y = size.height - barHeight
                drawRect(
                    color = if (hours >= target) goodColor else badColor,
                    topLeft = Offset(x, y),
                    size =
                        androidx.compose.ui.geometry
                            .Size(widthPerBar, barHeight),
                )
            }

            // Draw target line
            val targetY = size.height - (target / maxHours) * size.height * 0.8f
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
    // minimal preview with empty data just to render canvas
    WorklogBarChartScreen()
}
