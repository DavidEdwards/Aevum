@file:OptIn(ExperimentalMaterial3Api::class)

package dae.aevum.ui.composables.users

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import dae.aevum.R
import dae.aevum.ui.composables.reusable.CircleButton
import dae.aevum.ui.models.UserUiModel
import dae.aevum.utils.SampleUserUiModelProvider
import timber.log.Timber

@Preview(widthDp = 320)
@Composable
fun UserCard(
    @PreviewParameter(SampleUserUiModelProvider::class, 2) model: UserUiModel,
    modifier: Modifier = Modifier,
    selectUser: (Int) -> Unit = {},
    deleteUser: (Int) -> Unit = {},
) {
    Card(
        modifier = modifier
            .fillMaxWidth(),
        onClick = {
            Timber.v("Clicked on user ${model.id}")
            selectUser(model.id)
        },
        colors = CardDefaults.cardColors(
            containerColor = if (model.active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (model.active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically)
            ) {
                Text(
                    text = model.user
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
            ) {
                CircleButton(
                    active = model.active,
                    icon = Icons.Outlined.Clear,
                    iconContentDescription = stringResource(R.string.delete_pending_worklog),
                    inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    activeColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Timber.v("Deleting user ${model.id}")
                    deleteUser(model.id)
                }
            }
        }
    }
}