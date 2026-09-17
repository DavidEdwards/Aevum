package dae.aevum.ui.composables.reusable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dae.aevum.common.R
import dae.aevum.utils.WorklogTimeHelpers
import java.util.Locale

/**
 * Compact today / week progress for timesheet monitoring.
 */
@Composable
fun TimesheetProgressBanner(
    todayHours: Double,
    weekHours: Double,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val todayTarget = WorklogTimeHelpers.WEEKDAY_TARGET_HOURS
    val weekTarget = WorklogTimeHelpers.WEEK_TARGET_HOURS
    val todayProgress = (todayHours / todayTarget).toFloat().coerceIn(0f, 1f)
    val weekProgress = (weekHours / weekTarget).toFloat().coerceIn(0f, 1f)
    val weekRemaining = (weekTarget - weekHours).coerceAtLeast(0.0)

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = if (compact) 4.dp else 8.dp),
    ) {
        ProgressRow(
            label = stringResource(R.string.today_progress),
            valueText =
                String.format(
                    Locale.US,
                    "%.1f / %.1f h",
                    todayHours,
                    todayTarget,
                ),
            progress = todayProgress,
            compact = compact,
        )
        Spacer(modifier = Modifier.height(if (compact) 4.dp else 8.dp))
        ProgressRow(
            label = stringResource(R.string.week_progress),
            valueText =
                String.format(
                    Locale.US,
                    "%.1f / %.1f h (%s)",
                    weekHours,
                    weekTarget,
                    stringResource(R.string.hours_remaining, String.format(Locale.US, "%.1f", weekRemaining)),
                ),
            progress = weekProgress,
            compact = compact,
        )
    }
}

@Composable
private fun ProgressRow(
    label: String,
    valueText: String,
    progress: Float,
    compact: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.titleSmall,
        )
        Text(
            text = valueText,
            style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium,
        )
    }
    Spacer(modifier = Modifier.height(4.dp))
    LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier.fillMaxWidth(),
    )
}
