package dae.aevum.domain.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dae.aevum.utils.WorklogTimeHelpers
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.timesheetDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "timesheet_preferences",
)

data class TimesheetTargets(
    val weekdayTargetHours: Double = WorklogTimeHelpers.WEEKDAY_TARGET_HOURS,
    val weekTargetHours: Double = WorklogTimeHelpers.WEEK_TARGET_HOURS,
)

@Singleton
class TimesheetPreferences
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) {
        val targets: Flow<TimesheetTargets> =
            context.timesheetDataStore.data.map { prefs ->
                TimesheetTargets(
                    weekdayTargetHours =
                        prefs[KEY_WEEKDAY_HOURS] ?: WorklogTimeHelpers.WEEKDAY_TARGET_HOURS,
                    weekTargetHours =
                        prefs[KEY_WEEK_HOURS] ?: WorklogTimeHelpers.WEEK_TARGET_HOURS,
                )
            }

        suspend fun setWeekdayTargetHours(hours: Double) {
            context.timesheetDataStore.edit { prefs ->
                prefs[KEY_WEEKDAY_HOURS] = hours.coerceIn(1.0, 24.0)
            }
        }

        suspend fun setWeekTargetHours(hours: Double) {
            context.timesheetDataStore.edit { prefs ->
                prefs[KEY_WEEK_HOURS] = hours.coerceIn(1.0, 168.0)
            }
        }

        companion object {
            private val KEY_WEEKDAY_HOURS = doublePreferencesKey("weekday_target_hours")
            private val KEY_WEEK_HOURS = doublePreferencesKey("week_target_hours")
        }
    }
