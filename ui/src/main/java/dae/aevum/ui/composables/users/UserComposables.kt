package dae.aevum.ui.composables.users

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    selectUser: (Int) -> Unit = {},
    openSettings: (Int) -> Unit = {},
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (model.active) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                contentColor =
                    if (model.active) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .clickable {
                            Timber.v("Clicked on user ${model.id}")
                            selectUser(model.id)
                        }.padding(vertical = 8.dp),
            ) {
                Text(text = model.user)
            }

            IconButton(
                onClick = {
                    Timber.v("Opening settings for user ${model.id}")
                    openSettings(model.id)
                },
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = stringResource(R.string.user_settings_cd, model.user),
                )
            }
        }
    }
}
