package dae.aevum.ui.composables.dialogs

import android.Manifest.permission.RECORD_AUDIO
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import dae.aevum.common.R
import dae.aevum.ui.viewmodels.WorklogViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CompletedWorklogDialog(
    modifier: Modifier = Modifier,
    viewModel: WorklogViewModel = hiltViewModel(),
    navController: NavController,
    worklogId: Long,
) {
    val micPermissionState = rememberPermissionState(RECORD_AUDIO)
    val isStoppingWorklog by viewModel.isStoppingWorklog.collectAsState(initial = false)

    AlertDialog(
        modifier = modifier,
        onDismissRequest = {
            navController.popBackStack()
        },
        title = {
            Text(text = stringResource(R.string.worklog_summary))
        },
        text = {
            Column {
                Text(stringResource(R.string.worklog_summary_details))
                Spacer(modifier = Modifier.height(8.dp))

                if (isStoppingWorklog) {
                    CircularProgressIndicator()
                }
            }
        },
        confirmButton = {
            Row {
                val scope = rememberCoroutineScope()

                Button(
                    enabled = !isStoppingWorklog,
                    onClick = {
                        // Await stop before navigating, so the active worklog is finalized and summary persisted
                        scope.launch {
                            viewModel.stopLoggingForAwait(worklogId, "")
                            navController.navigate("worklogEditor/$worklogId")
                        }
                    },
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.write_log),
                    )
                    Text(stringResource(R.string.write_log))
                }

                Spacer(modifier = Modifier.width(8.dp))
                if (micPermissionState.status.isGranted) {
                    Button(
                        enabled = !isStoppingWorklog,
                        onClick = {
                            scope.launch {
                                val summary = viewModel.stopLoggingAudioLogAwait(worklogId)
                                if (summary.isBlank()) {
                                    navController.navigate("worklogEditor/$worklogId")
                                } else {
                                    navController.popBackStack()
                                }
                            }
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = stringResource(R.string.speak_log),
                        )
                        Text(stringResource(R.string.speak_log))
                    }
                } else {
                    Button(
                        enabled = !isStoppingWorklog,
                        onClick = { micPermissionState.launchPermissionRequest() },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = stringResource(R.string.speak_log),
                        )
                        Text(stringResource(R.string.speak_log))
                    }
                }
            }
        },
        dismissButton = {
            Button(
                enabled = !isStoppingWorklog,
                onClick = {
                    viewModel.deleteLog(worklogId) {
                        navController.popBackStack()
                    }
                },
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = stringResource(R.string.do_not_log),
                )
                Text(stringResource(R.string.do_not_log))
            }
        },
    )
}
