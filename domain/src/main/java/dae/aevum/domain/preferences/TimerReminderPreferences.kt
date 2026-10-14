package dae.aevum.domain.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dae.aevum.utils.VibrationPattern
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.timerReminderDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "timer_reminder_preferences",
)

enum class VibrationIntensity {
    SOFT,
    DEFAULT,
    STRONG,
    ;

    /** Amplitude for [android.os.VibrationEffect]; -1 = platform default. */
    val amplitude: Int
        get() =
            when (this) {
                SOFT -> 64
                DEFAULT -> -1
                STRONG -> 255
            }

    companion object {
        fun fromStored(value: String?): VibrationIntensity =
            entries.firstOrNull { it.name == value } ?: DEFAULT
    }
}

data class TimerReminderSettings(
    val enabled: Boolean = true,
    /** Minutes between reminder vibrations while a timer is running. */
    val intervalMinutes: Int = DEFAULT_INTERVAL_MINUTES,
    /** Length of the short pulse; long pulses are scaled from this. */
    val vibrationDurationMs: Long = DEFAULT_DURATION_MS,
    val intensity: VibrationIntensity = VibrationIntensity.DEFAULT,
    val pattern: VibrationPattern = VibrationPattern.TRIPLE,
) {
    val intervalMs: Long get() = intervalMinutes * 60_000L

    companion object {
        const val DEFAULT_INTERVAL_MINUTES = 15
        const val MIN_INTERVAL_MINUTES = 1
        const val MAX_INTERVAL_MINUTES = 180
        const val DEFAULT_DURATION_MS = 320L
        const val MIN_DURATION_MS = 50L
        const val MAX_DURATION_MS = 2_000L

        fun clampIntervalMinutes(minutes: Int): Int =
            minutes.coerceIn(MIN_INTERVAL_MINUTES, MAX_INTERVAL_MINUTES)

        fun clampDurationMs(durationMs: Long): Long =
            durationMs.coerceIn(MIN_DURATION_MS, MAX_DURATION_MS)
    }
}

@Singleton
class TimerReminderPreferences
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) {
        val settings: Flow<TimerReminderSettings> =
            context.timerReminderDataStore.data.map { prefs ->
                TimerReminderSettings(
                    enabled = prefs[KEY_ENABLED] ?: true,
                    intervalMinutes =
                        TimerReminderSettings.clampIntervalMinutes(
                            prefs[KEY_INTERVAL_MINUTES]
                                ?: TimerReminderSettings.DEFAULT_INTERVAL_MINUTES,
                        ),
                    vibrationDurationMs =
                        TimerReminderSettings.clampDurationMs(
                            prefs[KEY_DURATION_MS]
                                ?: TimerReminderSettings.DEFAULT_DURATION_MS,
                        ),
                    intensity =
                        VibrationIntensity.fromStored(prefs[KEY_INTENSITY]),
                    pattern = VibrationPattern.fromStored(prefs[KEY_PATTERN]),
                )
            }

        suspend fun setEnabled(enabled: Boolean) {
            context.timerReminderDataStore.edit { prefs ->
                prefs[KEY_ENABLED] = enabled
            }
        }

        suspend fun setIntervalMinutes(minutes: Int) {
            context.timerReminderDataStore.edit { prefs ->
                prefs[KEY_INTERVAL_MINUTES] =
                    TimerReminderSettings.clampIntervalMinutes(minutes)
            }
        }

        suspend fun setVibrationDurationMs(durationMs: Long) {
            context.timerReminderDataStore.edit { prefs ->
                prefs[KEY_DURATION_MS] =
                    TimerReminderSettings.clampDurationMs(durationMs)
            }
        }

        suspend fun setIntensity(intensity: VibrationIntensity) {
            context.timerReminderDataStore.edit { prefs ->
                prefs[KEY_INTENSITY] = intensity.name
            }
        }

        suspend fun setPattern(pattern: VibrationPattern) {
            context.timerReminderDataStore.edit { prefs ->
                prefs[KEY_PATTERN] = pattern.name
            }
        }

        companion object {
            private val KEY_ENABLED = booleanPreferencesKey("enabled")
            private val KEY_INTERVAL_MINUTES = intPreferencesKey("interval_minutes")
            private val KEY_DURATION_MS = longPreferencesKey("duration_ms")
            private val KEY_INTENSITY = stringPreferencesKey("intensity")
            private val KEY_PATTERN = stringPreferencesKey("pattern")
        }
    }
