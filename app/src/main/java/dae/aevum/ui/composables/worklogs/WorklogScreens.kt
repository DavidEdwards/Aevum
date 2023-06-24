package dae.aevum.ui.composables.worklogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dae.aevum.R
import dae.aevum.ui.composables.reusable.PullToRefreshList
import dae.aevum.ui.viewmodels.WorklogViewModel
import dae.aevum.utils.human
import java.time.Duration
import java.time.Instant

@Composable
fun WorklogListScreen(
    modifier: Modifier = Modifier,
    viewModel: WorklogViewModel = hiltViewModel(),
    onNavigateToEditor: (Long) -> Unit,
    onUpdate: (Long, Instant, Instant, String) -> Unit = { _, _, _, _ -> },
    stopLogging: (Long) -> Unit = {},
    deleteWorklog: (Long) -> Unit = {},
) {
    val worklogs by viewModel.pendingWorklogs.collectAsState(initial = emptyList())


    PullToRefreshList(
        modifier = modifier
            .padding(horizontal = 16.dp),
        result = worklogs
    ) { models ->
        AlreadyLoggedTodayList()

        if (models.isEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.no_pending_worklogs),
                style = MaterialTheme.typography.bodyLarge
            )
        } else {
            models.forEach { model ->
                Spacer(modifier = Modifier.height(8.dp))
                WorklogCard(
                    worklogUiModel = model,
                    onNavigate = {
                        onNavigateToEditor.invoke(it.workId)
                    },
                    onUpdate = onUpdate,
                    stopLogging = stopLogging,
                    deleteWorklog = deleteWorklog
                )
            }
        }
    }
}

@Composable
fun AlreadyLoggedTodayList(
    modifier: Modifier = Modifier,
    viewModel: WorklogViewModel = hiltViewModel()
) {
    val worklogsToday by viewModel.worklogsToday.collectAsState(initial = emptyList())

    if (worklogsToday.isNotEmpty()) {
        Column(
            modifier = modifier
        ) {
            Text(
                text = stringResource(R.string.worklogs_posted_today_already),
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(8.dp))

            worklogsToday.forEach { model ->
                val duration = Duration.between(model.from, model.to).human()
                Text(
                    text = "${model.issueId.value}: $duration",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            val totalDuration =
                worklogsToday.map { model -> Duration.between(model.from, model.to) }
                    .fold(Duration.ZERO) { d1: Duration, d2: Duration ->
                        d1.plus(d2)
                    }.human()
            Text(
                text = "Total: $totalDuration",
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun WorklogEditorScreen(
    modifier: Modifier = Modifier,
    viewModel: WorklogViewModel = hiltViewModel(),
    worklogId: Long?,
    updateWorklog: (Long, Instant, Instant, String) -> Unit = { _, _, _, _ -> },
) {
    viewModel.setViewedWorklogId(worklogId)
    val model by viewModel.worklogBeingViewed.collectAsState(initial = null)
    if (model == null) {
        WorklogInvalid()
        return
    }

    Column(
        modifier = modifier
            .padding(16.dp)
            .fillMaxSize()
    ) {
        WorklogDetails(
            modifier = Modifier,
            model = model!!,
            updateWorklog = updateWorklog
        )
    }
}

@Composable
fun WorklogInvalid() {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize()
    ) {
        Text(
            text = stringResource(R.string.worklog_not_available),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}