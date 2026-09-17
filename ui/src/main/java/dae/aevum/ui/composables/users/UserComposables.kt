@file:OptIn(ExperimentalMaterial3Api::class)

package dae.aevum.ui.composables.users

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import dae.aevum.common.R
import dae.aevum.ui.models.UserUiModel
import dae.aevum.ui.preview.SampleUserUiModelProvider
import timber.log.Timber

@Preview(widthDp = 320)
@Composable
fun UserCard(
    @PreviewParameter(SampleUserUiModelProvider::class, 2) model: UserUiModel,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    selectUser: (Int) -> Unit = {},
    deleteUser: (Int) -> Unit = {},
    updateUserToken: (Int, String, (Boolean) -> Unit) -> Unit = { _, _, _ -> },
) {
    val accountNotVerifiedError = stringResource(R.string.account_not_verified)

    var menuExpanded by rememberSaveable { mutableStateOf(false) }
    var showUpdateDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var token by rememberSaveable { mutableStateOf("") }
    var updateTokenError by rememberSaveable { mutableStateOf("") }

    Card(
        modifier =
            modifier
                .fillMaxWidth(),
        onClick = {
            Timber.v("Clicked on user ${model.id}")
            selectUser(model.id)
        },
        colors =
            CardDefaults.cardColors(
                containerColor = if (model.active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (model.active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .align(Alignment.CenterVertically),
            ) {
                Text(
                    text = model.user,
                )
            }

            Box(
                modifier =
                    Modifier
                        .align(Alignment.CenterVertically),
            ) {
                IconButton(
                    onClick = {
                        menuExpanded = true
                    },
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = stringResource(R.string.user_actions),
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = {
                        menuExpanded = false
                    },
                ) {
                    DropdownMenuItem(
                        text = { Text(text = stringResource(R.string.update_token)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Key,
                                contentDescription = null,
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            updateTokenError = ""
                            token = ""
                            showUpdateDialog = true
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(text = stringResource(R.string.delete_user)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = null,
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            showDeleteDialog = true
                        },
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
            },
            title = {
                Text(text = stringResource(R.string.delete_user))
            },
            text = {
                Text(text = stringResource(R.string.delete_user_confirm, model.user))
            },
            confirmButton = {
                Button(
                    onClick = {
                        Timber.v("Deleting user ${model.id}")
                        showDeleteDialog = false
                        deleteUser(model.id)
                    },
                ) {
                    Text(text = stringResource(R.string.delete_user))
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                    },
                ) {
                    Text(text = stringResource(R.string.close))
                }
            },
        )
    }

    if (showUpdateDialog) {
        AlertDialog(
            onDismissRequest = {
                showUpdateDialog = false
            },
            title = {
                Text(text = stringResource(R.string.update_token))
            },
            text = {
                Column {
                    if (updateTokenError.isNotBlank()) {
                        Text(
                            text = updateTokenError,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = token,
                        onValueChange = {
                            token = it
                            updateTokenError = ""
                        },
                        label = {
                            Text(text = stringResource(R.string.new_token))
                        },
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = !loading,
                    onClick = {
                        updateUserToken(model.id, token) { success ->
                            if (success) {
                                showUpdateDialog = false
                            } else {
                                updateTokenError = accountNotVerifiedError
                            }
                        }
                    },
                ) {
                    Text(text = stringResource(R.string.save_token))
                }
            },
            dismissButton = {
                Button(
                    enabled = !loading,
                    onClick = {
                        showUpdateDialog = false
                    },
                ) {
                    Text(text = stringResource(R.string.close))
                }
            }
        )
    }
}
