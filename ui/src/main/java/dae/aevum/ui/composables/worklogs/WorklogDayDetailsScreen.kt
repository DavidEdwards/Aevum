package dae.aevum.ui.composables.worklogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Divider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dae.aevum.ui.models.WorklogIssueUiModel
import dae.aevum.ui.viewmodels.WorklogViewModel
import dae.aevum.utils.Formatters
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun WorklogDayDetailsScreen(
    viewModel: WorklogViewModel = hiltViewModel(),
    day: LocalDate? = null,
) {
    val worklogs by viewModel.worklogIssuesLastTwoWeeks.collectAsState(initial = emptyList())
    WorklogDayDetailsContent(entries = worklogs, day = day)
}

@Composable
private fun WorklogDayDetailsContent(
    entries: List<WorklogIssueUiModel>,
    day: LocalDate? = null,
) {
    val grouped = entries.groupBy { it.from.atZone(ZoneId.systemDefault()).toLocalDate() }
    val orderedDays =
        if (day != null) listOf(day) else (0..13).map { LocalDate.now().minusDays(it.toLong()) }

    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
        contentPadding =
            androidx.compose.foundation.layout
                .PaddingValues(bottom = 96.dp),
    ) {
        items(orderedDays) { selectedDay ->
            val entriesForDay = grouped[selectedDay].orEmpty()
            val issueKeysForDay = entriesForDay.map { it.issueId.value }.distinct().sorted()
            val colorMapForDay = generateColorMap(issueKeysForDay)
            val nowState =
                remember(selectedDay, entriesForDay.any { it.active }) {
                    mutableStateOf<Instant?>(null)
                }
            LaunchedEffect(selectedDay, entriesForDay.any { it.active }) {
                if (selectedDay == LocalDate.now() && entriesForDay.any { it.active }) {
                    while (true) {
                        nowState.value = Instant.now()
                        delay(1_000L)
                    }
                } else {
                    nowState.value = null
                }
            }

            val displayEntries =
                entriesForDay.map { e ->
                    if (e.active) e.copy(to = nowState.value ?: e.to) else e
                }
            val totalSeconds = displayEntries.sumOf { Duration.between(it.from, it.to).seconds }
            val hours = totalSeconds / 3600.0

            // Day header card with progress
            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        val title = selectedDay.format(DateTimeFormatter.ISO_LOCAL_DATE)
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = String.format("%.1f h", hours),
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            textAlign = TextAlign.End,
                            modifier = Modifier.widthIn(min = 80.dp),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    SegmentedProgressBar(displayEntries, colorMapForDay)
                }
            }

            // Worklog items
            displayEntries.forEachIndexed { index, e ->
                val duration = Duration.between(e.from, e.to)
                val ehours = duration.seconds / 3600.0
                val start = Formatters.universalTimeFormatter.format(e.from)
                val end = Formatters.universalTimeFormatter.format(e.to)

                val isActive = e.active
                val contentOnColor =
                    if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                val badgeBackground =
                    if (isActive) {
                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                    } else {
                        MaterialTheme.colorScheme.primary.copy(
                            alpha = 0.2f,
                        )
                    }

                Card(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                    colors =
                        CardDefaults.cardColors(
                            containerColor =
                                if (isActive) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                            contentColor =
                                if (isActive) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                        ),
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Spacer(Modifier.width(5.dp))
                                Box(
                                    modifier =
                                        Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Color.Black,
                                            ).padding(1.dp)
                                            .clip(CircleShape)
                                            .background(
                                                colorMapForDay[e.issueId.value]
                                                    ?: MaterialTheme.colorScheme.primary,
                                            ),
                                )
                                Spacer(Modifier.width(13.dp))
                                Text(
                                    text = e.issueTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            // Duration badge
                            Box(
                                modifier =
                                    Modifier
                                        .clip(
                                            androidx.compose.foundation.shape
                                                .RoundedCornerShape(12.dp),
                                        ).background(badgeBackground)
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    text = String.format("%.1f h", ehours),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = contentOnColor,
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        // Time range
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.AccessTime,
                                contentDescription = null,
                                tint = contentOnColor,
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "$start — $end",
                                style = MaterialTheme.typography.bodyMedium,
                                color = contentOnColor,
                            )
                        }
                        if (e.author.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.Person,
                                    contentDescription = null,
                                    tint = contentOnColor,
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = e.author,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = contentOnColor,
                                )
                            }
                        }
                        if (e.comment.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    imageVector = Icons.Outlined.Description,
                                    contentDescription = null,
                                    tint = contentOnColor,
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(text = e.comment, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                if (index != entriesForDay.lastIndex) {
                    Divider(modifier = Modifier.padding(vertical = 2.dp), color = Color.Transparent)
                }
            }
        }
    }
}

@Composable
private fun SegmentedProgressBar(
    entriesForDay: List<WorklogIssueUiModel>,
    colorMap: Map<String, Color>,
) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    if (entriesForDay.isEmpty()) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(
                        androidx.compose.foundation.shape
                            .RoundedCornerShape(4.dp),
                    ).background(trackColor),
        )
        return
    }

    val sorted = entriesForDay.sortedBy { it.from }
    val spanStart = sorted.first().from
    val spanEnd = sorted.last().to
    val totalMillis = Duration.between(spanStart, spanEnd).toMillis().coerceAtLeast(1)

    data class Piece(
        val fraction: Float,
        val filled: Boolean,
    )

    val pieces = mutableListOf<Pair<Piece, String>>()
    var cursor = spanStart
    sorted.forEach { e ->
        // Gap before this segment
        val gapMillis = Duration.between(cursor, e.from).toMillis().coerceAtLeast(0)
        if (gapMillis > 0) {
            pieces.add(
                Pair(
                    Piece(gapMillis.toFloat() / totalMillis.toFloat(), filled = false),
                    e.issueId.value,
                ),
            )
        }

        // Filled segment
        val segMillis = Duration.between(e.from, e.to).toMillis().coerceAtLeast(0)
        if (segMillis > 0) {
            pieces.add(
                Pair(
                    Piece(segMillis.toFloat() / totalMillis.toFloat(), filled = true),
                    e.issueId.value,
                ),
            )
        }
        cursor = e.to
    }
    // Tail gap
    val tailMillis = Duration.between(cursor, spanEnd).toMillis().coerceAtLeast(0)
    if (tailMillis > 0) {
        pieces.add(
            Pair(
                Piece(tailMillis.toFloat() / totalMillis.toFloat(), filled = false),
                sorted.last().issueId.value,
            ),
        )
    }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(
                    androidx.compose.foundation.shape
                        .RoundedCornerShape(4.dp),
                ).background(trackColor)
                .border(
                    width = 1.dp,
                    color = Color.Black,
                    shape =
                        androidx.compose.foundation.shape
                            .RoundedCornerShape(4.dp),
                ),
    ) {
        val visiblePieces = pieces.filter { it.first.fraction > 0f }
        visiblePieces.forEachIndexed { index, (piece, issueKey) ->
            Box(
                modifier =
                    Modifier
                        .weight(piece.fraction)
                        .fillMaxHeight()
                        .background(
                            if (piece.filled) {
                                (
                                    colorMap[issueKey]
                                        ?: MaterialTheme.colorScheme.primary
                                )
                            } else {
                                trackColor
                            },
                        ),
            )

            if (index != visiblePieces.lastIndex) {
                Box(
                    modifier =
                        Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(Color.Black),
                )
            }
        }
    }
}

private fun generateColorMap(issueKeys: List<String>): Map<String, Color> {
    if (issueKeys.isEmpty()) return emptyMap()
    val phiConjugate = 0.61803398875f
    val map = HashMap<String, Color>(issueKeys.size)
    var hueSeed =
        (issueKeys.joinToString("|").hashCode().toLong() and 0x7FFFFFFF).toFloat() / Int.MAX_VALUE
    if (hueSeed.isNaN()) hueSeed = 0.0f
    var hue = hueSeed
    issueKeys.forEachIndexed { index, key ->
        hue = (hue + phiConjugate) % 1.0f
        val saturation = 0.60f + ((index % 3) * 0.12f)
        val lightness = 0.50f - ((index % 2) * 0.10f)
        map[key] = hslToColor(hue, saturation.coerceIn(0f, 1f), lightness.coerceIn(0f, 1f))
    }
    return map
}

private fun hslToColor(
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
    val r = ((r1 + m) * 255f).toInt().coerceIn(0, 255)
    val g = ((g1 + m) * 255f).toInt().coerceIn(0, 255)
    val b = ((b1 + m) * 255f).toInt().coerceIn(0, 255)
    val argb = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    return Color(argb)
}

@Preview
@Composable
private fun WorklogDayDetailsScreenPreview() {
    val now = Instant.now()
    val sample =
        listOf(
            WorklogIssueUiModel(
                workId = 1,
                issueId = dae.aevum.utils.IssueId("AAPP-1"),
                author = "You",
                from = now.minusSeconds(5 * 3600),
                to = now.minusSeconds(3 * 3600),
                comment = "Implemented feature X",
                pending = true,
                active = true,
                issueTitle = "AAPP-1",
            ),
            WorklogIssueUiModel(
                workId = 2,
                issueId = dae.aevum.utils.IssueId("AAPP-2"),
                author = "You",
                from = now.minusSeconds(2 * 3600),
                to = now.minusSeconds(1 * 3600),
                comment = "Code review",
                pending = false,
                active = false,
                issueTitle = "AAPP-2",
            ),
        )
    WorklogDayDetailsContent(entries = sample, day = LocalDate.now())
}
