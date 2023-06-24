package dae.aevum.ui.composables.reusable

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dae.aevum.R
import dae.aevum.ui.viewmodels.IssueViewModel
import dae.aevum.utils.IssueId
import dae.aevum.utils.SampleIssueUiModelProvider

@Preview(widthDp = 320, showBackground = true)
@Composable
fun StartStopLoggingButton(
    modifier: Modifier = Modifier,
    @PreviewParameter(SampleIssueUiModelProvider::class, 1) issue: IssueId,
    viewModel: IssueViewModel = hiltViewModel(),
    startLogging: (IssueId) -> Unit = {},
    stopLogging: (Long) -> Unit = {},
) {
    val activeIssue by viewModel.activeIssue.collectAsState(initial = null)

    if (activeIssue == null) {
        OutlinedButton(
            modifier = modifier,
            onClick = {
//            viewModel.startLogFor(issue.id)
                startLogging(issue)
            }) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = stringResource(R.string.start_logging)
            )
            Text(text = stringResource(R.string.start_logging))
        }
    } else {
        OutlinedButton(
            modifier = modifier,
            onClick = {
//            viewModel.stopLogging("A summary")
                stopLogging(activeIssue!!.worklog.workId)
            }) {
            Icon(
                imageVector = Icons.Default.Done,
                contentDescription = stringResource(
                    R.string.stop_logging_to,
                    activeIssue!!.issue.id.value
                )
            )
            Text(text = stringResource(R.string.stop_logging_to, activeIssue!!.issue.id.value))
        }
    }
}

@Composable
fun CircleButton(
    modifier: Modifier = Modifier,
    active: Boolean,
    icon: ImageVector,
    iconContentDescription: String,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.onSurface,
    onButtonClick: () -> Unit
) {
    val color = if (active) {
        activeColor
    } else {
        inactiveColor
    }
    val borderSize = if (active) {
        3.dp
    } else {
        1.dp
    }

    OutlinedButton(
        modifier = modifier.size(48.dp),
        contentPadding = PaddingValues(0.dp),
        border = BorderStroke(borderSize, color),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
        onClick = {
            onButtonClick()
        },
        shape = CircleShape
    ) {
        Icon(
            imageVector = icon,
            contentDescription = iconContentDescription
        )
    }
}