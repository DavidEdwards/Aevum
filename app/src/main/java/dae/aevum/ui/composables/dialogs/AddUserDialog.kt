package dae.aevum.ui.composables.dialogs

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import dae.aevum.R
import dae.aevum.ui.viewmodels.UserViewModel
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import timber.log.Timber

@Composable
fun AddUserDialog(
    modifier: Modifier = Modifier,
    viewModel: UserViewModel = hiltViewModel(),
    navController: NavController = rememberNavController()
) {
    var instanceUrl by rememberSaveable {
        mutableStateOf(viewModel.instanceUrl)
    }
    var user by rememberSaveable {
        mutableStateOf(viewModel.user)
    }
    var token by rememberSaveable {
        mutableStateOf(viewModel.token)
    }
    var error by rememberSaveable {
        mutableStateOf("")
    }
    Timber.v("Error: $error")

    AlertDialog(
        modifier = modifier,
        onDismissRequest = {
            navController.popBackStack()
        },
        title = {
            Text(text = stringResource(R.string.user_details))
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(state = rememberScrollState())
            ) {
                if (error.isNotBlank()) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Text(stringResource(R.string.user_details_info))
                }

                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = instanceUrl,
                    onValueChange = { data ->
                        instanceUrl = data
                        viewModel.instanceUrl = instanceUrl
                        error = ""
                    },
                    label = {
                        Text(text = stringResource(R.string.instance_url))
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                )

                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = user,
                    onValueChange = { data ->
                        user = data
                        viewModel.user = instanceUrl
                        error = ""
                    },
                    label = {
                        Text(text = stringResource(R.string.user))
                    }
                )

                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = token,
                    onValueChange = { data ->
                        token = data
                        viewModel.token = instanceUrl
                        error = ""
                    },
                    label = {
                        Text(text = stringResource(R.string.token))
                    }
                )

                val context = LocalContext.current
                OutlinedButton(onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                        data = "https://id.atlassian.com/manage-profile/security/api-tokens".toUri()
                    })
                }) {
                    Text(text = stringResource(R.string.get_a_token))
                }
            }
        },
        confirmButton = {
            Column {
                val accountNotVerifiedError = stringResource(R.string.account_not_verified)
                val badInstanceUrlError = stringResource(R.string.bad_instance_url)
                if (viewModel.loading) {
                    CircularProgressIndicator()
                } else {
                    Button(
                        onClick = {
                            instanceUrl.toHttpUrlOrNull() ?: run {
                                error = badInstanceUrlError
                                return@Button
                            }

                            viewModel.addUser(instanceUrl, user, token) { success ->
                                if (success) {
                                    navController.popBackStack()
                                } else {
                                    error = accountNotVerifiedError
                                }
                            }
                        }) {
                        Icon(
                            imageVector = Icons.Default.Done,
                            contentDescription = stringResource(R.string.add_user)
                        )
                        Text(stringResource(R.string.add_user))
                    }
                }
            }
        },
        dismissButton = {
            Button(
                onClick = {
                    navController.popBackStack()
                }) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = stringResource(R.string.close)
                )
                Text(stringResource(R.string.close))
            }
        }
    )
}