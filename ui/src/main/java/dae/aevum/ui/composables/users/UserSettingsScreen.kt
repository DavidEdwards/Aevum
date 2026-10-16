@file:OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)

package dae.aevum.ui.composables.users

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import dae.aevum.common.R
import dae.aevum.domain.preferences.ProofreaderConfig
import dae.aevum.domain.preferences.ProofreaderProvider
import dae.aevum.domain.preferences.TimerReminderSettings
import dae.aevum.domain.preferences.TimesheetTargets
import dae.aevum.domain.preferences.VibrationIntensity
import dae.aevum.ui.composables.reusable.listBottomContentPadding
import dae.aevum.ui.models.UserUiModel
import dae.aevum.ui.theme.AppTheme
import dae.aevum.ui.viewmodels.UserViewModel
import dae.aevum.utils.TimerReminderVibration
import dae.aevum.utils.VibrationPattern
import dae.aevum.utils.WorklogTimeHelpers
import kotlinx.coroutines.delay
import java.time.DayOfWeek

@Composable
fun UserSettingsScreen(
    userId: Int,
    modifier: Modifier = Modifier,
    viewModel: UserViewModel = hiltViewModel(),
    onUserDeleted: () -> Unit = {},
) {
    val users by viewModel.users.collectAsState(initial = emptyList())
    val user = users.find { it.id == userId }
    val targets by viewModel.timesheetTargets.collectAsState()
    val proofreader by viewModel.proofreaderConfig.collectAsState()
    val timerReminder by viewModel.timerReminderSettings.collectAsState()
    val loading = viewModel.loading

    if (user == null) {
        Text(
            text = stringResource(R.string.no_active_user_error),
            modifier = modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyLarge,
        )
        return
    }

    UserSettingsContent(
        model = user,
        targets = targets,
        proofreader = proofreader,
        timerReminder = timerReminder,
        loading = loading,
        modifier = modifier,
        onUpdateToken = { token, after ->
            viewModel.updateUserToken(userId, token, after)
        },
        onHoursPerDayChange = { viewModel.setWeekdayTargetHours(it) },
        onToggleWorkingDay = { viewModel.toggleWorkingDay(it) },
        onTimerReminderEnabledChange = { viewModel.setTimerReminderEnabled(it) },
        onTimerReminderIntervalChange = { viewModel.setTimerReminderIntervalMinutes(it) },
        onTimerReminderDurationChange = { viewModel.setTimerReminderDurationMs(it) },
        onTimerReminderIntensityChange = { viewModel.setTimerReminderIntensity(it) },
        onTimerReminderPatternChange = { viewModel.setTimerReminderPattern(it) },
        onProofreaderProviderChange = { viewModel.setProofreaderProvider(it) },
        onProofreaderUrlChange = { viewModel.setProofreaderEndpointUrl(it) },
        onProofreaderModelChange = { viewModel.setProofreaderModel(it) },
        onProofreaderApiKeyChange = { viewModel.setProofreaderApiKey(it) },
        onProofreaderConnectionTimeoutChange = {
            viewModel.setProofreaderConnectionTimeoutSeconds(it)
        },
        onDeleteUser = {
            viewModel.removeUser(userId) {
                onUserDeleted()
            }
        },
    )
}

@Composable
fun UserSettingsContent(
    model: UserUiModel,
    targets: TimesheetTargets,
    proofreader: ProofreaderConfig,
    timerReminder: TimerReminderSettings = TimerReminderSettings(),
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    onUpdateToken: (String, (Boolean) -> Unit) -> Unit = { _, _ -> },
    onHoursPerDayChange: (Double) -> Unit = {},
    onToggleWorkingDay: (DayOfWeek) -> Unit = {},
    onTimerReminderEnabledChange: (Boolean) -> Unit = {},
    onTimerReminderIntervalChange: (Int) -> Unit = {},
    onTimerReminderDurationChange: (Long) -> Unit = {},
    onTimerReminderIntensityChange: (VibrationIntensity) -> Unit = {},
    onTimerReminderPatternChange: (VibrationPattern) -> Unit = {},
    onProofreaderProviderChange: (ProofreaderProvider) -> Unit = {},
    onProofreaderUrlChange: (String) -> Unit = {},
    onProofreaderModelChange: (String) -> Unit = {},
    onProofreaderApiKeyChange: (String) -> Unit = {},
    onProofreaderConnectionTimeoutChange: (Int) -> Unit = {},
    onDeleteUser: () -> Unit = {},
) {
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = model.user,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (model.instanceUrl.isNotBlank()) {
            Text(
                text = model.instanceUrl,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        AccountSection(
            loading = loading,
            onUpdateToken = onUpdateToken,
        )

        WorkScheduleSection(
            hoursPerDay = targets.weekdayTargetHours,
            workingDays = targets.workingDays,
            weekHours = targets.weekTargetHours,
            onHoursPerDayChange = onHoursPerDayChange,
            onToggleWorkingDay = onToggleWorkingDay,
        )

        TimerReminderSection(
            settings = timerReminder,
            onEnabledChange = onTimerReminderEnabledChange,
            onIntervalChange = onTimerReminderIntervalChange,
            onDurationChange = onTimerReminderDurationChange,
            onIntensityChange = onTimerReminderIntensityChange,
            onPatternChange = onTimerReminderPatternChange,
        )

        ProofreaderSection(
            config = proofreader,
            onProviderChange = onProofreaderProviderChange,
            onUrlChange = onProofreaderUrlChange,
            onModelChange = onProofreaderModelChange,
            onApiKeyChange = onProofreaderApiKeyChange,
            onConnectionTimeoutChange = onProofreaderConnectionTimeoutChange,
        )

        DangerZoneSection(
            onDeleteClick = { showDeleteDialog = true },
        )

        Spacer(modifier = Modifier.height(listBottomContentPadding()))
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(text = stringResource(R.string.delete_user)) },
            text = {
                Text(text = stringResource(R.string.delete_user_confirm, model.user))
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteUser()
                    },
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        ),
                ) {
                    Text(text = stringResource(R.string.delete_user))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tonal: Boolean = false,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (tonal) {
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    },
            ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint =
                        if (tonal) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    modifier = Modifier.size(22.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            content()
        }
    }
}

@Composable
private fun AccountSection(
    loading: Boolean,
    onUpdateToken: (String, (Boolean) -> Unit) -> Unit,
) {
    val accountNotVerifiedError = stringResource(R.string.account_not_verified)
    var token by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf("") }
    var savedFlash by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(savedFlash) {
        if (savedFlash) {
            delay(2000)
            savedFlash = false
        }
    }

    SettingsSectionCard(
        title = stringResource(R.string.settings_account),
        subtitle = stringResource(R.string.settings_account_subtitle),
        icon = Icons.Outlined.Key,
    ) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = token,
            onValueChange = {
                token = it
                error = ""
                savedFlash = false
            },
            label = { Text(stringResource(R.string.new_token)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            isError = error.isNotBlank(),
            supportingText =
                when {
                    error.isNotBlank() -> {
                        { Text(error) }
                    }

                    savedFlash -> {
                        { Text(stringResource(R.string.token_updated)) }
                    }

                    else -> {
                        null
                    }
                },
        )
        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = !loading && token.isNotBlank(),
            onClick = {
                onUpdateToken(token) { success ->
                    if (success) {
                        token = ""
                        error = ""
                        savedFlash = true
                    } else {
                        error = accountNotVerifiedError
                    }
                }
            },
        ) {
            Text(stringResource(R.string.save_token))
        }
    }
}

@Composable
private fun WorkScheduleSection(
    hoursPerDay: Double,
    workingDays: Set<DayOfWeek>,
    weekHours: Double,
    onHoursPerDayChange: (Double) -> Unit,
    onToggleWorkingDay: (DayOfWeek) -> Unit,
) {
    var hoursText by remember(hoursPerDay) {
        mutableStateOf(
            if (hoursPerDay == hoursPerDay.toLong().toDouble()) {
                hoursPerDay.toLong().toString()
            } else {
                hoursPerDay.toString()
            },
        )
    }
    val dayLabels = stringArrayResource(R.array.weekday_short_mon_sun)
    val orderedDays = DayOfWeek.entries // Monday=1 … Sunday=7 in java.time

    SettingsSectionCard(
        title = stringResource(R.string.settings_work_schedule),
        subtitle = stringResource(R.string.settings_work_schedule_subtitle),
        icon = Icons.Outlined.Schedule,
    ) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = hoursText,
            onValueChange = { text ->
                hoursText = text.filter { it.isDigit() || it == '.' }
                hoursText.toDoubleOrNull()?.let { onHoursPerDayChange(it) }
            },
            label = { Text(stringResource(R.string.hours_per_day)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            suffix = { Text("h") },
        )

        Text(
            text = stringResource(R.string.working_days),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            orderedDays.forEach { day ->
                val selected = day in workingDays
                val labelIndex = day.value - 1
                FilterChip(
                    selected = selected,
                    onClick = { onToggleWorkingDay(day) },
                    label = { Text(dayLabels.getOrElse(labelIndex) { day.name.take(3) }) },
                    colors =
                        FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                )
            }
        }

        Text(
            text = stringResource(R.string.hours_per_week_computed, weekHours),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun TimerReminderSection(
    settings: TimerReminderSettings,
    onEnabledChange: (Boolean) -> Unit,
    onIntervalChange: (Int) -> Unit,
    onDurationChange: (Long) -> Unit,
    onIntensityChange: (VibrationIntensity) -> Unit,
    onPatternChange: (VibrationPattern) -> Unit,
) {
    val context = LocalContext.current
    var intervalText by remember(settings.intervalMinutes) {
        mutableStateOf(settings.intervalMinutes.toString())
    }
    var durationText by remember(settings.vibrationDurationMs) {
        mutableStateOf(settings.vibrationDurationMs.toString())
    }

    SettingsSectionCard(
        title = stringResource(R.string.settings_timer_reminder),
        subtitle = stringResource(R.string.settings_timer_reminder_subtitle),
        icon = Icons.Outlined.Vibration,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.timer_reminder_enabled),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = settings.enabled,
                onCheckedChange = onEnabledChange,
            )
        }

        AnimatedVisibility(visible = settings.enabled) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = intervalText,
                    onValueChange = { text ->
                        intervalText = text.filter { it.isDigit() }
                        intervalText.toIntOrNull()?.let { onIntervalChange(it) }
                    },
                    label = { Text(stringResource(R.string.timer_reminder_interval)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    suffix = { Text(stringResource(R.string.timer_reminder_interval_suffix)) },
                )

                Text(
                    text = stringResource(R.string.timer_reminder_pattern),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    VibrationPattern.entries.forEach { pattern ->
                        FilterChip(
                            selected = settings.pattern == pattern,
                            onClick = { onPatternChange(pattern) },
                            label = { Text(patternLabel(pattern)) },
                            colors =
                                FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                ),
                        )
                    }
                }

                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = durationText,
                    onValueChange = { text ->
                        durationText = text.filter { it.isDigit() }
                        durationText.toLongOrNull()?.let { onDurationChange(it) }
                    },
                    label = { Text(stringResource(R.string.timer_reminder_duration)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    suffix = { Text(stringResource(R.string.timer_reminder_duration_suffix)) },
                )

                Text(
                    text = stringResource(R.string.timer_reminder_intensity),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    VibrationIntensity.entries.forEach { intensity ->
                        FilterChip(
                            selected = settings.intensity == intensity,
                            onClick = { onIntensityChange(intensity) },
                            label = { Text(intensityLabel(intensity)) },
                            colors =
                                FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                ),
                        )
                    }
                }

                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { vibratePreview(context, settings) },
                ) {
                    Text(stringResource(R.string.timer_reminder_test))
                }
            }
        }
    }
}

@Composable
private fun patternLabel(pattern: VibrationPattern): String =
    when (pattern) {
        VibrationPattern.SINGLE -> stringResource(R.string.timer_reminder_pattern_single)
        VibrationPattern.DOUBLE -> stringResource(R.string.timer_reminder_pattern_double)
        VibrationPattern.TRIPLE -> stringResource(R.string.timer_reminder_pattern_triple)
        VibrationPattern.SHORT_LONG_SHORT ->
            stringResource(R.string.timer_reminder_pattern_short_long_short)
        VibrationPattern.LONG_SHORT_LONG ->
            stringResource(R.string.timer_reminder_pattern_long_short_long)
        VibrationPattern.HEARTBEAT -> stringResource(R.string.timer_reminder_pattern_heartbeat)
    }

@Composable
private fun intensityLabel(intensity: VibrationIntensity): String =
    when (intensity) {
        VibrationIntensity.SOFT -> stringResource(R.string.timer_reminder_intensity_soft)
        VibrationIntensity.DEFAULT -> stringResource(R.string.timer_reminder_intensity_default)
        VibrationIntensity.STRONG -> stringResource(R.string.timer_reminder_intensity_strong)
    }

/** Host app declares [android.Manifest.permission.VIBRATE]. */
@SuppressLint("MissingPermission")
private fun vibratePreview(
    context: Context,
    settings: TimerReminderSettings,
) {
    try {
        val vibrator =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(VibratorManager::class.java).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
        val shortPulse =
            TimerReminderSettings.clampDurationMs(settings.vibrationDurationMs)
        vibrator.vibrate(
            TimerReminderVibration.createEffect(
                timings = settings.pattern.timings(shortPulse),
                amplitude = settings.intensity.amplitude,
            ),
        )
    } catch (_: Exception) {
        // Best-effort preview; hardware may lack a vibrator.
    }
}

@Composable
private fun ProofreaderSection(
    config: ProofreaderConfig,
    onProviderChange: (ProofreaderProvider) -> Unit,
    onUrlChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onConnectionTimeoutChange: (Int) -> Unit,
) {
    var urlText by remember(config.endpointUrl) { mutableStateOf(config.endpointUrl) }
    var modelText by remember(config.model) { mutableStateOf(config.model) }
    var apiKeyText by remember(config.apiKey) { mutableStateOf(config.apiKey) }
    var timeoutText by remember(config.connectionTimeoutSeconds) {
        mutableStateOf(config.connectionTimeoutSeconds.toString())
    }
    val networkProvider =
        config.provider == ProofreaderProvider.OLLAMA ||
            config.provider == ProofreaderProvider.OPENAI_COMPATIBLE

    SettingsSectionCard(
        title = stringResource(R.string.settings_proofreader),
        subtitle = stringResource(R.string.settings_proofreader_subtitle),
        icon = Icons.Outlined.SmartToy,
    ) {
        Text(
            text = stringResource(R.string.proofreader_provider),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ProofreaderProvider.entries.forEach { provider ->
                FilterChip(
                    selected = config.provider == provider,
                    onClick = { onProviderChange(provider) },
                    label = { Text(providerLabel(provider)) },
                    colors =
                        FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                )
            }
        }

        val hint = providerHint(config.provider)
        if (hint != null) {
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        AnimatedVisibility(visible = config.provider == ProofreaderProvider.OLLAMA) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .onFocusChanged { focus ->
                                if (!focus.isFocused && urlText != config.endpointUrl) {
                                    onUrlChange(urlText)
                                }
                            },
                    value = urlText,
                    onValueChange = { urlText = it },
                    label = { Text(stringResource(R.string.proofreader_endpoint_url)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                )
                OutlinedTextField(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .onFocusChanged { focus ->
                                if (!focus.isFocused && modelText != config.model) {
                                    onModelChange(modelText)
                                }
                            },
                    value = modelText,
                    onValueChange = { modelText = it },
                    label = { Text(stringResource(R.string.proofreader_model)) },
                    singleLine = true,
                )
            }
        }

        AnimatedVisibility(visible = config.provider == ProofreaderProvider.OPENAI_COMPATIBLE) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OpenAiPresetChip(
                        label = stringResource(R.string.proofreader_preset_openai),
                        onClick = {
                            urlText = ProofreaderConfig.DEFAULT_OPENAI_URL
                            modelText = ProofreaderConfig.DEFAULT_OPENAI_MODEL
                            onUrlChange(urlText)
                            onModelChange(modelText)
                        },
                    )
                    OpenAiPresetChip(
                        label = stringResource(R.string.proofreader_preset_openrouter),
                        onClick = {
                            urlText = "https://openrouter.ai/api/v1"
                            modelText = "openai/gpt-4o-mini"
                            onUrlChange(urlText)
                            onModelChange(modelText)
                        },
                    )
                    OpenAiPresetChip(
                        label = stringResource(R.string.proofreader_preset_groq),
                        onClick = {
                            urlText = "https://api.groq.com/openai/v1"
                            modelText = "llama-3.3-70b-versatile"
                            onUrlChange(urlText)
                            onModelChange(modelText)
                        },
                    )
                }
                OutlinedTextField(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .onFocusChanged { focus ->
                                if (!focus.isFocused && urlText != config.endpointUrl) {
                                    onUrlChange(urlText)
                                }
                            },
                    value = urlText,
                    onValueChange = { urlText = it },
                    label = { Text(stringResource(R.string.proofreader_base_url)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                )
                OutlinedTextField(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .onFocusChanged { focus ->
                                if (!focus.isFocused && apiKeyText != config.apiKey) {
                                    onApiKeyChange(apiKeyText)
                                }
                            },
                    value = apiKeyText,
                    onValueChange = { apiKeyText = it },
                    label = { Text(stringResource(R.string.proofreader_api_key)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                )
                OutlinedTextField(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .onFocusChanged { focus ->
                                if (!focus.isFocused && modelText != config.model) {
                                    onModelChange(modelText)
                                }
                            },
                    value = modelText,
                    onValueChange = { modelText = it },
                    label = { Text(stringResource(R.string.proofreader_model)) },
                    singleLine = true,
                )
            }
        }

        AnimatedVisibility(visible = networkProvider) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = timeoutText,
                onValueChange = { text ->
                    timeoutText = text.filter { it.isDigit() }
                    timeoutText.toIntOrNull()?.let { onConnectionTimeoutChange(it) }
                },
                label = { Text(stringResource(R.string.proofreader_connection_timeout)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                suffix = {
                    Text(stringResource(R.string.proofreader_connection_timeout_suffix))
                },
                supportingText = {
                    Text(stringResource(R.string.proofreader_connection_timeout_hint))
                },
            )
        }
    }
}

@Composable
private fun OpenAiPresetChip(
    label: String,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = false,
        onClick = onClick,
        label = { Text(label) },
    )
}

@Composable
private fun providerLabel(provider: ProofreaderProvider): String =
    when (provider) {
        ProofreaderProvider.OFF -> stringResource(R.string.proofreader_provider_off)
        ProofreaderProvider.OLLAMA -> stringResource(R.string.proofreader_provider_ollama)
        ProofreaderProvider.OPENAI_COMPATIBLE -> stringResource(R.string.proofreader_provider_openai)
        ProofreaderProvider.KOOG_ON_DEVICE -> stringResource(R.string.proofreader_provider_koog)
    }

@Composable
private fun providerHint(provider: ProofreaderProvider): String? =
    when (provider) {
        ProofreaderProvider.OFF -> null
        ProofreaderProvider.OLLAMA -> stringResource(R.string.proofreader_provider_ollama_hint)
        ProofreaderProvider.OPENAI_COMPATIBLE -> stringResource(R.string.proofreader_provider_openai_hint)
        ProofreaderProvider.KOOG_ON_DEVICE -> stringResource(R.string.proofreader_provider_koog_hint)
    }

@Composable
private fun DangerZoneSection(onDeleteClick: () -> Unit) {
    SettingsSectionCard(
        title = stringResource(R.string.settings_danger_zone),
        subtitle = stringResource(R.string.settings_danger_zone_subtitle),
        icon = Icons.Outlined.Delete,
        tonal = true,
    ) {
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = onDeleteClick,
            colors =
                ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
        ) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(stringResource(R.string.delete_user))
        }
    }
}

@Preview(widthDp = 360, showBackground = true)
@Composable
private fun UserSettingsPreview() {
    AppTheme {
        UserSettingsContent(
            model =
                UserUiModel(
                    id = 1,
                    user = "alice@example.com",
                    instanceUrl = "https://example.atlassian.net",
                    active = true,
                ),
            targets =
                TimesheetTargets(
                    weekdayTargetHours = 8.0,
                    workingDays = WorklogTimeHelpers.DEFAULT_WORKING_DAYS,
                ),
            proofreader =
                ProofreaderConfig(
                    provider = ProofreaderProvider.OLLAMA,
                    endpointUrl = ProofreaderConfig.DEFAULT_OLLAMA_URL,
                    model = ProofreaderConfig.DEFAULT_OLLAMA_MODEL,
                ),
            timerReminder = TimerReminderSettings(),
        )
    }
}
