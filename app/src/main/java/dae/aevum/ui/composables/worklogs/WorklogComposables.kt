@file:OptIn(
    ExperimentalMaterial3Api::class
)

package dae.aevum.ui.composables.worklogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import dae.aevum.App
import dae.aevum.R
import dae.aevum.ui.composables.reusable.CircleButton
import dae.aevum.ui.models.WorklogUiModel
import dae.aevum.utils.SampleWorklogUiModelProvider
import dae.aevum.utils.human
import kotlinx.coroutines.delay
import timber.log.Timber
import java.time.Duration
import java.time.Instant

@Preview(widthDp = 320)
@Composable
fun WorklogCard(
    @PreviewParameter(SampleWorklogUiModelProvider::class, 1) worklogUiModel: WorklogUiModel,
    modifier: Modifier = Modifier,
    onNavigate: (WorklogUiModel) -> Unit = {},
    onUpdate: (Long, Instant, Instant, String) -> Unit = { _, _, _, _ -> },
    stopLogging: (Long) -> Unit = {},
    deleteWorklog: (Long) -> Unit = {},
) {
    var worklogTo by remember {
        mutableStateOf(worklogUiModel.to)
    }

    LaunchedEffect(worklogUiModel.workId, worklogUiModel.active) {
        while (worklogUiModel.active) {
            Timber.v("Monitoring the worklog duration for ${worklogUiModel.workId}")

            val now = Instant.now()
            Timber.v("current=${worklogTo.epochSecond} now=${now.epochSecond} comment=${worklogUiModel.comment}")
            if (worklogTo.epochSecond != now.epochSecond) {
                worklogTo = now
            }
            delay(1_000L)
        }
        worklogTo = worklogUiModel.to
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = !worklogUiModel.active) {
                Timber.v("Clicked on worklog")

                if (!worklogUiModel.active) {
                    onNavigate(worklogUiModel)
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = if (worklogUiModel.active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (worklogUiModel.active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                TimeLabel(from = worklogUiModel.from, to = worklogTo)
                Spacer(modifier = Modifier.height(8.dp))
                if (worklogUiModel.active) {
                    Text(text = stringResource(R.string.logging_to_x, worklogUiModel.issueId.value))
                } else {
                    Text(text = worklogUiModel.author)
                    Text(text = worklogUiModel.comment)
                }
            }

            if (worklogUiModel.active) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .align(Alignment.CenterVertically)
                ) {
                    CircleButton(
                        active = false,
                        icon = Icons.Outlined.Done,
                        iconContentDescription = stringResource(R.string.stop_logging),
                        inactiveColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        stopLogging(worklogUiModel.workId)
                    }
                }
            } else if (worklogUiModel.pending) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .align(Alignment.CenterVertically)
                ) {
                    CircleButton(
                        active = false,
                        icon = Icons.Outlined.Clear,
                        iconContentDescription = stringResource(R.string.delete_pending_worklog)
                    ) {
                        deleteWorklog(worklogUiModel.workId)
                    }
                }
            }
        }
    }
}

@Composable
fun TimeLabel(
    modifier: Modifier = Modifier,
    from: Instant,
    to: Instant
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
    ) {
        Column {
            Row {
                Text(
                    text = App.universalDateFormatter.format(from),
                    maxLines = 1
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "•",
                    maxLines = 1
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = App.universalTimeFormatter.format(from),
                    maxLines = 1
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "•",
                    maxLines = 1
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = Duration.between(from, to).human(),
                    maxLines = 1
                )
            }
        }
    }
}

@Preview(widthDp = 320, showBackground = true)
@Composable
fun WorklogDetails(
    modifier: Modifier = Modifier,
    @PreviewParameter(SampleWorklogUiModelProvider::class, 1) model: WorklogUiModel,
    updateWorklog: (Long, Instant, Instant, String) -> Unit = { _, _, _, _ -> },
    splitWorklog: (Long) -> Unit = {}
) {
    var summary by remember {
        mutableStateOf(model.comment)
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            OutlinedButton(
                modifier = modifier,
                onClick = {
                    updateWorklog(model.workId, model.from, model.to, summary)
                }) {
                Icon(
                    imageVector = Icons.Default.Done,
                    contentDescription = stringResource(R.string.save)
                )
                Text(text = stringResource(R.string.save))
            }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(
                modifier = modifier,
                onClick = {
                    splitWorklog(model.workId)
                }) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(R.string.split)
                )
                Text(text = stringResource(R.string.split))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = stringResource(R.string.issue_id_x, model.issueId.value))
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = stringResource(R.string.author_x, model.author))
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = summary,
            onValueChange = { data ->
                summary = data
            },
            label = {
                Text(text = stringResource(R.string.summary))
            }
        )
    }
}