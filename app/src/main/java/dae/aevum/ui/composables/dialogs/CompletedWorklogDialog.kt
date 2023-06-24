package dae.aevum.ui.composables.dialogs

import android.Manifest.permission.RECORD_AUDIO
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import dae.aevum.R
import dae.aevum.ui.viewmodels.WorklogViewModel

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CompletedWorklogDialog(
    modifier: Modifier = Modifier,
    viewModel: WorklogViewModel = hiltViewModel(),
    navController: NavController,
    worklogId: Long,
) {
    val micPermissionState = rememberPermissionState(RECORD_AUDIO)

    AlertDialog(
        modifier = modifier,
        onDismissRequest = {
            navController.popBackStack()
        },
        title = {
            Text(text = stringResource(R.string.worklog_summary))
        },
        text = {
            Text(stringResource(R.string.worklog_summary_details))
        },
        confirmButton = {
            Column {
                Button(
                    onClick = {
                        viewModel.stopLogging("") {
//                            navController.popBackStack()
                            navController.navigate("worklogEditor/${worklogId}")
                        }
                    }) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.write_log)
                    )
                    Text(stringResource(R.string.write_log))
                }


                if (micPermissionState.status.isGranted) {
                    Button(
                        onClick = {
                            viewModel.stopLoggingAudioLog {
                                navController.popBackStack()
                            }
                        }) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = stringResource(R.string.speak_log)
                        )
                        Text(stringResource(R.string.speak_log))
                    }
                } else {
                    Button(onClick = { micPermissionState.launchPermissionRequest() }) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = stringResource(R.string.speak_log)
                        )
                        Text(stringResource(R.string.speak_log))
                    }
                }
            }
        },
        dismissButton = {
            Button(
                onClick = {
                    viewModel.deleteLog(worklogId) {
                        navController.popBackStack()
                    }
                }) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = stringResource(R.string.do_not_log)
                )
                Text(stringResource(R.string.do_not_log))
            }
        }
    )
}