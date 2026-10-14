package dae.aevum.ui.composables.worklogs

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dae.aevum.common.R
import dae.aevum.ui.models.WorklogIssueUiModel
import dae.aevum.ui.models.WorklogUiModel
import dae.aevum.utils.Formatters
import dae.aevum.utils.IssueId
import dae.aevum.utils.WorklogTimeHelpers
import dae.aevum.utils.human
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.math.min

data class PendingIssueSlice(
    val issueId: String,
    val millis: Long,
    val color: Color,
)

@Composable
fun PendingBreakdownChart(
    pending: List<WorklogUiModel>,
    modifier: Modifier = Modifier,
) {
    if (pending.isEmpty()) return

    var now by remember { mutableStateOf(Instant.now()) }
    val hasActive = pending.any { it.active }
    LaunchedEffect(hasActive) {
        if (hasActive) {
            while (true) {
                now = Instant.now()
                delay(1_000L)
            }
        } else {
            now = Instant.now()
        }
    }

    val slices =
        remember(pending, now) {
            val grouped =
                pending
                    .groupBy { it.issueId.value }
                    .mapValues { (_, items) ->
                        items.sumOf { item ->
                            val to = WorklogTimeHelpers.effectiveTo(item.from, item.to, item.active, now)
                            Duration.between(item.from, to).toMillis().coerceAtLeast(0L)
                        }
                    }.filterValues { it > 0L }
                    .toList()
                    .sortedByDescending { it.second }
            val colors = pendingIssueColors(grouped.map { it.first })
            grouped.map { (issueId, millis) ->
                PendingIssueSlice(
                    issueId = issueId,
                    millis = millis,
                    color = colors.getValue(issueId),
                )
            }
        }

    if (slices.isEmpty()) return

    val totalMillis = slices.sumOf { it.millis }.coerceAtLeast(1L)
    val totalLabel = Duration.ofMillis(totalMillis).human()

    Card(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.pending_breakdown),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.pending_total, totalLabel),
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PendingDonutChart(
                    slices = slices,
                    totalMillis = totalMillis,
                    modifier = Modifier.size(96.dp),
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    PendingStackedBar(
                        slices = slices,
                        totalMillis = totalMillis,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(14.dp),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    slices.forEach { slice ->
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(slice.color),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = slice.issueId,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = Duration.ofMillis(slice.millis).human(),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingDonutChart(
    slices: List<PendingIssueSlice>,
    totalMillis: Long,
    modifier: Modifier = Modifier,
) {
    val track = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = size.minDimension * 0.18f, cap = StrokeCap.Butt)
        val diameter = size.minDimension
        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
        val arcSize = Size(diameter, diameter)
        drawArc(
            color = track,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = stroke,
        )
        var startAngle = -90f
        slices.forEach { slice ->
            val sweep = (slice.millis.toFloat() / totalMillis.toFloat()) * 360f
            drawArc(
                color = slice.color,
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke,
            )
            startAngle += sweep
        }
    }
}

@Composable
private fun PendingStackedBar(
    slices: List<PendingIssueSlice>,
    totalMillis: Long,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .clip(RoundedCornerShape(7.dp))
                .background(MaterialTheme.colorScheme.outlineVariant),
    ) {
        slices.forEach { slice ->
            val weight = (slice.millis.toFloat() / totalMillis.toFloat()).coerceAtLeast(0.01f)
            Box(
                modifier =
                    Modifier
                        .weight(weight)
                        .fillMaxHeight()
                        .background(slice.color),
            )
        }
    }
}

@Composable
fun PostedWorklogCard(
    model: WorklogIssueUiModel,
    modifier: Modifier = Modifier,
) {
    val start = Formatters.universalTimeFormatter.format(model.from)
    val end = Formatters.universalTimeFormatter.format(model.to)
    val duration = Duration.between(model.from, model.to).human()
    val secondary = MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = model.issueId.value,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = duration,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            if (model.issueTitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = model.issueTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.AccessTime,
                    contentDescription = null,
                    tint = secondary,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.time_range_x, start, end),
                    style = MaterialTheme.typography.bodyMedium,
                    color = secondary,
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Outlined.Description,
                    contentDescription = null,
                    tint = secondary,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text =
                        model.comment.ifBlank {
                            stringResource(R.string.no_comment)
                        },
                    style = MaterialTheme.typography.bodySmall,
                    color =
                        if (model.comment.isBlank()) {
                            secondary.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                )
            }
        }
    }
}

@Composable
fun PendingSectionHeader(
    title: String,
    trailing: String? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        if (trailing != null) {
            Text(
                text = trailing,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

internal fun pendingIssueColors(issueKeys: List<String>): Map<String, Color> {
    if (issueKeys.isEmpty()) return emptyMap()
    val phiConjugate = 0.61803398875f
    val map = HashMap<String, Color>(issueKeys.size)
    var hue =
        (issueKeys.joinToString("|").hashCode().toLong() and 0x7FFFFFFF).toFloat() / Int.MAX_VALUE
    if (hue.isNaN()) hue = 0.0f
    issueKeys.forEachIndexed { index, key ->
        hue = (hue + phiConjugate) % 1.0f
        val saturation = 0.62f + ((index % 3) * 0.10f)
        val lightness = 0.52f - ((index % 2) * 0.08f)
        map[key] = hslToPendingColor(hue, saturation.coerceIn(0f, 1f), lightness.coerceIn(0f, 1f))
    }
    return map
}

private fun hslToPendingColor(
    h: Float,
    s: Float,
    l: Float,
): Color {
    val c = (1f - kotlin.math.abs(2 * l - 1f)) * s
    val hPrime = h * 6f
    val x = c * (1f - kotlin.math.abs((hPrime % 2f) - 1f))
    val (r1, g1, b1) =
        when {
            hPrime < 1f -> Triple(c, x, 0f)
            hPrime < 2f -> Triple(x, c, 0f)
            hPrime < 3f -> Triple(0f, c, x)
            hPrime < 4f -> Triple(0f, x, c)
            hPrime < 5f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
    val m = l - c / 2f
    return Color(
        red = min(1f, r1 + m),
        green = min(1f, g1 + m),
        blue = min(1f, b1 + m),
    )
}

@Preview(widthDp = 360, showBackground = true)
@Composable
private fun PendingBreakdownChartPreview() {
    val now = Instant.now()
    PendingBreakdownChart(
        pending =
            listOf(
                WorklogUiModel(
                    workId = 1,
                    issueId = IssueId("AAPP-12"),
                    author = "Pending",
                    from = now.minus(90, ChronoUnit.MINUTES),
                    to = now,
                    comment = "",
                    pending = true,
                    active = true,
                ),
                WorklogUiModel(
                    workId = 2,
                    issueId = IssueId("AAPP-8"),
                    author = "Pending",
                    from = now.minus(3, ChronoUnit.HOURS),
                    to = now.minus(2, ChronoUnit.HOURS),
                    comment = "Review PR",
                    pending = true,
                    active = false,
                ),
            ),
    )
}

@Preview(widthDp = 360, showBackground = true)
@Composable
private fun PostedWorklogCardPreview() {
    val now = Instant.now()
    PostedWorklogCard(
        model =
            WorklogIssueUiModel(
                workId = 3,
                issueId = IssueId("AAPP-3"),
                author = "Ada",
                from = now.minus(5, ChronoUnit.HOURS),
                to = now.minus(4, ChronoUnit.HOURS),
                comment = "Fixed login timeout handling",
                pending = false,
                active = false,
                issueTitle = "Handle session expiry gracefully",
            ),
    )
}
