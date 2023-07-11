package dae.aevum.ui.composables.issues

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dae.aevum.R
import dae.aevum.ui.composables.reusable.CircleButton
import dae.aevum.ui.composables.reusable.PullToRefreshList
import dae.aevum.ui.composables.reusable.StartStopLoggingButton
import dae.aevum.ui.composables.worklogs.WorklogCard
import dae.aevum.ui.models.IssueUiModel
import dae.aevum.ui.viewmodels.IssueViewModel
import dae.aevum.utils.IssueId
import dae.aevum.utils.SampleIssueUiModelProvider
import timber.log.Timber
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Preview(widthDp = 320)
@Composable
fun IssueCard(
    @PreviewParameter(SampleIssueUiModelProvider::class, 1) issue: IssueUiModel,
    modifier: Modifier = Modifier,
    onNavigateToIssue: (IssueId) -> Unit = {},
    onTogglePinIssue: ((IssueId) -> Unit)? = null,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        onClick = {
            Timber.v("Clicked on issue")
            onNavigateToIssue(issue.id)
        }
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
                Text(
                    text = issue.id.value,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = issue.title,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            if (onTogglePinIssue != null) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .align(Alignment.CenterVertically)
                ) {
                    CircleButton(
                        active = issue.pinned,
                        icon = Icons.Outlined.Place,
                        iconContentDescription = stringResource(R.string.pin_this_issue)
                    ) {
                        onTogglePinIssue(issue.id)
                    }
                }
            }
        }
    }
}

@Preview(widthDp = 320, showBackground = true)
@Composable
fun IssueDetails(
    modifier: Modifier = Modifier,
    @PreviewParameter(SampleIssueUiModelProvider::class, 1) model: IssueUiModel,
    viewModel: IssueViewModel = hiltViewModel(),
    startLogging: (IssueId) -> Unit = {},
    stopLogging: (Long) -> Unit = {},
    onUpdate: (Long, Instant, Instant, String) -> Unit = { _, _, _, _ -> },
    deleteWorklog: (Long) -> Unit = {},
) {
    LaunchedEffect(Unit) {
        Timber.v("Reload issue screen")
        viewModel.refreshWorklogsFor(model.id)
    }

    val worklogs by viewModel.worklogsForViewedIssue.collectAsState(initial = emptyList())

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Row {
            Text(
                text = model.id.value + " · " + model.title,
                style = MaterialTheme.typography.titleLarge
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        StartStopLoggingButton(
            issue = model.id,
            startLogging = startLogging,
            stopLogging = stopLogging
        )

        PullToRefreshList(result = worklogs, refresh = {
            viewModel.refreshWorklogsFor(model.id)
        }) { result ->
            result.forEach { worklogUiModel ->
                Spacer(modifier = Modifier.height(8.dp))
                WorklogCard(
                    worklogUiModel = worklogUiModel,
                    onUpdate = onUpdate,
                    stopLogging = stopLogging,
                    deleteWorklog = deleteWorklog
                )
            }
        }
    }
}