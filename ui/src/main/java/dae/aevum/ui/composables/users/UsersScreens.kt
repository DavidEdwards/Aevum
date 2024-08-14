package dae.aevum.ui.composables.users

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dae.aevum.ui.viewmodels.UserViewModel

@Composable
fun UserListScreen(
    modifier: Modifier = Modifier,
    viewModel: UserViewModel = hiltViewModel(),
    selectUser: (Int) -> Unit = {},
    deleteUser: (Int) -> Unit = {},
) {
    val userModels by viewModel.users.collectAsState(initial = emptyList())

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(state = rememberScrollState())
    ) {
        userModels.forEach { model ->
            UserCard(
                model = model,
                selectUser = selectUser,
                deleteUser = deleteUser
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
