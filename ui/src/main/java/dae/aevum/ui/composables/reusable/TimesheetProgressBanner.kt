package dae.aevum.ui.composables.reusable

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dae.aevum.common.R
import dae.aevum.utils.WorklogTimeHelpers
import java.time.LocalDate
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
    weekdayTargetHours: Double = WorklogTimeHelpers.WEEKDAY_TARGET_HOURS,
    weekTargetHours: Double = WorklogTimeHelpers.WEEK_TARGET_HOURS,
    onClick: (() -> Unit)? = null,
) {
    val todayTarget = WorklogTimeHelpers.dayTargetHours(LocalDate.now(), weekdayTargetHours)
    val weekTarget = weekTargetHours
    val todayProgress = WorklogTimeHelpers.progressFraction(todayHours, todayTarget)
    val weekProgress = WorklogTimeHelpers.progressFraction(weekHours, weekTarget)
    val weekRemaining = (weekTarget - weekHours).coerceAtLeast(0.0)
    val todayOvertime = WorklogTimeHelpers.overtimeHours(todayHours, todayTarget)
    val weekOvertime = WorklogTimeHelpers.overtimeHours(weekHours, weekTarget)

    val todayValue =
        buildString {
            append(String.format(Locale.US, "%.1f / %.1f h", todayHours, todayTarget))
            if (todayOvertime > 0.0) {
                append(" ")
                append(String.format(Locale.US, "+%.1f h", todayOvertime))
            }
        }
    val weekValue =
        buildString {
            append(String.format(Locale.US, "%.1f / %.1f h", weekHours, weekTarget))
            if (weekOvertime > 0.0) {
                append(String.format(Locale.US, " (+%.1f h)", weekOvertime))
            } else {
                append(
                    " (${stringResource(R.string.hours_remaining, String.format(Locale.US, "%.1f", weekRemaining))})",
                )
            }
        }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .then(
                    if (onClick != null) {
                        Modifier.clickable(onClick = onClick)
                    } else {
                        Modifier
                    },
                )
                .padding(vertical = if (compact) 4.dp else 8.dp),
    ) {
        ProgressRow(
            label = stringResource(R.string.today_progress),
            valueText = todayValue,
            progress = todayProgress,
            overTarget = todayOvertime > 0.0,
            compact = compact,
        )
        Spacer(modifier = Modifier.height(if (compact) 4.dp else 8.dp))
        ProgressRow(
            label = stringResource(R.string.week_progress),
            valueText = weekValue,
            progress = weekProgress,
            overTarget = weekOvertime > 0.0,
            compact = compact,
        )
    }
}

@Composable
private fun ProgressRow(
    label: String,
    valueText: String,
    progress: Float,
    overTarget: Boolean,
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
            color =
                if (overTarget) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    Color.Unspecified
                },
        )
    }
    Spacer(modifier = Modifier.height(4.dp))
    LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier.fillMaxWidth(),
        color =
            if (overTarget) {
                MaterialTheme.colorScheme.tertiary
            } else {
                MaterialTheme.colorScheme.primary
            },
    )
}
