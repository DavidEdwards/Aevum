package dae.aevum.ui.composables.issues

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dae.aevum.R
import dae.aevum.ui.composables.reusable.HighlightColors
import dae.aevum.ui.composables.reusable.PullToRefreshList
import dae.aevum.ui.composables.reusable.highlightSpanStyle
import dae.aevum.ui.composables.reusable.highlightText
import dae.aevum.ui.models.IssueUiModel
import dae.aevum.ui.viewmodels.IssueViewModel
import dae.aevum.utils.IssueId
import timber.log.Timber
import java.time.Instant

@Composable
fun IssueListScreen(
    modifier: Modifier = Modifier,
    viewModel: IssueViewModel = hiltViewModel(),
    onNavigateToIssue: (IssueId) -> Unit
) {
    val activeUser by viewModel.activeUser.collectAsState(initial = null)
    if (activeUser == null) {
        NoActiveUser()
        return
    }

    LaunchedEffect(Unit) {
        Timber.v("Reload issue list screen")
        viewModel.refreshIssues()
    }

    val searchResult by viewModel.filteredIssues.collectAsState()

    PullToRefreshList(
        modifier = modifier
            .padding(horizontal = 16.dp),
        result = searchResult, refresh = {
            viewModel.refreshIssues()
        }) { result ->
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = viewModel.searchTerm,
            label = {
                Text(text = stringResource(R.string.search_term))
            },
            onValueChange = { term ->
                viewModel.searchTerm = term
            }
        )

        val issueUiModels = result.map {
            val highlighted = highlightText(
                text = it.title,
                query = viewModel.searchTerm,
                span = highlightSpanStyle(
                    HighlightColors(
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        background = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            )
            IssueUiModel(it.id, highlighted, it.pinned)
        }

        if (issueUiModels.isEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.no_visible_issues),
                style = MaterialTheme.typography.bodyLarge
            )
        } else {
            issueUiModels.forEach { issueUiModel ->
                Spacer(modifier = Modifier.height(8.dp))
                IssueCard(
                    issue = issueUiModel,
                    onNavigateToIssue = onNavigateToIssue,
                    onTogglePinIssue = { issueId ->
                        viewModel.toggleIssuePin(issueId)
                    }
                )
            }
        }
    }
}

@Composable
fun IssueScreen(
    modifier: Modifier = Modifier,
    viewModel: IssueViewModel = hiltViewModel(),
    issueId: IssueId?,
    stopLogging: (Long) -> Unit = {},
    onUpdate: (Long, Instant, Instant, String) -> Unit = { _, _, _, _ -> },
    deleteWorklog: (Long) -> Unit = {},
) {
    viewModel.setViewedIssueId(issueId)
    val model by viewModel.issueBeingViewed.collectAsState(initial = null)
    if (model == null) {
        IssueInvalid()
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
    ) {
        IssueDetails(
            model = model!!,
            startLogging = {
                viewModel.startLogFor(it)
            },
            stopLogging = stopLogging,
            onUpdate = onUpdate,
            deleteWorklog = deleteWorklog
        )
    }
}

@Composable
fun IssueInvalid() {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize()
    ) {
        Text(
            text = stringResource(R.string.issue_not_available),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun NoActiveUser() {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize()
    ) {
        Text(
            text = stringResource(R.string.no_active_user_error),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}