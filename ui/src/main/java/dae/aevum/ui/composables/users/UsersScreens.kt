package dae.aevum.ui.composables.users

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import dae.aevum.common.R
import dae.aevum.ui.viewmodels.UserViewModel

@Composable
fun UserListScreen(
    modifier: Modifier = Modifier,
    viewModel: UserViewModel = hiltViewModel(),
    selectUser: (Int) -> Unit = {},
    deleteUser: (Int) -> Unit = {},
    updateUserToken: (Int, String, (Boolean) -> Unit) -> Unit = { _, _, _ -> },
) {
    val userModels by viewModel.users.collectAsState(initial = emptyList())
    val loading = viewModel.loading
    val targets by viewModel.timesheetTargets.collectAsState()

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(state = rememberScrollState()),
    ) {
        userModels.forEach { model ->
            UserCard(
                model = model,
                loading = loading,
                selectUser = selectUser,
                deleteUser = deleteUser,
                updateUserToken = updateUserToken,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.timesheet_targets),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(8.dp))
        TimesheetTargetsEditor(
            weekdayHours = targets.weekdayTargetHours,
            weekHours = targets.weekTargetHours,
            onWeekdayHoursChange = { viewModel.setWeekdayTargetHours(it) },
            onWeekHoursChange = { viewModel.setWeekTargetHours(it) },
        )
        Spacer(modifier = Modifier.height(96.dp))
    }
}

@Composable
private fun TimesheetTargetsEditor(
    weekdayHours: Double,
    weekHours: Double,
    onWeekdayHoursChange: (Double) -> Unit,
    onWeekHoursChange: (Double) -> Unit,
) {
    var weekdayText by remember(weekdayHours) {
        mutableStateOf(weekdayHours.toInt().toString())
    }
    var weekText by remember(weekHours) {
        mutableStateOf(weekHours.toInt().toString())
    }

    Row(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
            value = weekdayText,
            onValueChange = { text ->
                weekdayText = text.filter { it.isDigit() || it == '.' }
                weekdayText.toDoubleOrNull()?.let { onWeekdayHoursChange(it) }
            },
            label = { Text(stringResource(R.string.weekday_target_hours)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
        OutlinedTextField(
            modifier = Modifier.weight(1f),
            value = weekText,
            onValueChange = { text ->
                weekText = text.filter { it.isDigit() || it == '.' }
                weekText.toDoubleOrNull()?.let { onWeekHoursChange(it) }
            },
            label = { Text(stringResource(R.string.week_target_hours)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
    }
}
