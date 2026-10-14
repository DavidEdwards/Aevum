package dae.aevum.domain.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dae.aevum.utils.WorklogTimeHelpers
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

private val Context.timesheetDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "timesheet_preferences",
)

data class TimesheetTargets(
    val weekdayTargetHours: Double = WorklogTimeHelpers.WEEKDAY_TARGET_HOURS,
    val workingDays: Set<DayOfWeek> = WorklogTimeHelpers.DEFAULT_WORKING_DAYS,
) {
    /** Hours per week derived from hours/day × working days. */
    val weekTargetHours: Double =
        WorklogTimeHelpers.weekTargetHours(weekdayTargetHours, workingDays)
}

/**
 * Persisted summary-calendar sync state so already-fetched history is read from the DB
 * instead of being re-downloaded from Jira on every visit.
 */
data class SummaryHistoryState(
    val weekCount: Int = DEFAULT_SUMMARY_WEEKS,
    /** Exclusive upper bound of the last successful forward sync (`Instant.now()` at sync time). */
    val fetchedUntil: Instant? = null,
    /** Start of the oldest range successfully pulled from Jira for the calendar. */
    val oldestSyncedStart: Instant? = null,
) {
    companion object {
        const val DEFAULT_SUMMARY_WEEKS = 8
        const val MAX_SUMMARY_WEEKS = 52
    }
}

@Singleton
class TimesheetPreferences
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) {
        val targets: Flow<TimesheetTargets> =
            context.timesheetDataStore.data.map { prefs ->
                val workingDays =
                    WorklogTimeHelpers
                        .decodeWorkingDays(
                            prefs[KEY_WORKING_DAYS_MASK]
                                ?: WorklogTimeHelpers.DEFAULT_WORKING_DAYS_MASK,
                        ).ifEmpty { WorklogTimeHelpers.DEFAULT_WORKING_DAYS }
                TimesheetTargets(
                    weekdayTargetHours =
                        prefs[KEY_WEEKDAY_HOURS] ?: WorklogTimeHelpers.WEEKDAY_TARGET_HOURS,
                    workingDays = workingDays,
                )
            }

        val summaryHistory: Flow<SummaryHistoryState> =
            context.timesheetDataStore.data.map { prefs ->
                SummaryHistoryState(
                    weekCount =
                        (prefs[KEY_SUMMARY_WEEK_COUNT] ?: SummaryHistoryState.DEFAULT_SUMMARY_WEEKS)
                            .coerceIn(SummaryHistoryState.DEFAULT_SUMMARY_WEEKS, SummaryHistoryState.MAX_SUMMARY_WEEKS),
                    fetchedUntil = prefs[KEY_SUMMARY_FETCHED_UNTIL_MS]?.let(Instant::ofEpochMilli),
                    oldestSyncedStart =
                        prefs[KEY_SUMMARY_OLDEST_SYNCED_MS]?.let(Instant::ofEpochMilli),
                )
            }

        suspend fun setWeekdayTargetHours(hours: Double) {
            context.timesheetDataStore.edit { prefs ->
                prefs[KEY_WEEKDAY_HOURS] = hours.coerceIn(1.0, 24.0)
            }
        }

        /**
         * Replaces the working-day set. At least one day is required; an empty set is ignored.
         * Week target hours are derived automatically from hours/day × days.
         */
        suspend fun setWorkingDays(days: Set<DayOfWeek>) {
            if (days.isEmpty()) return
            context.timesheetDataStore.edit { prefs ->
                prefs[KEY_WORKING_DAYS_MASK] = WorklogTimeHelpers.encodeWorkingDays(days)
            }
        }

        suspend fun toggleWorkingDay(day: DayOfWeek) {
            context.timesheetDataStore.edit { prefs ->
                val current =
                    WorklogTimeHelpers
                        .decodeWorkingDays(
                            prefs[KEY_WORKING_DAYS_MASK]
                                ?: WorklogTimeHelpers.DEFAULT_WORKING_DAYS_MASK,
                        ).ifEmpty { WorklogTimeHelpers.DEFAULT_WORKING_DAYS }
                        .toMutableSet()
                if (day in current) {
                    if (current.size > 1) {
                        current.remove(day)
                    }
                } else {
                    current.add(day)
                }
                prefs[KEY_WORKING_DAYS_MASK] = WorklogTimeHelpers.encodeWorkingDays(current)
            }
        }

        suspend fun setSummaryWeekCount(weekCount: Int) {
            context.timesheetDataStore.edit { prefs ->
                prefs[KEY_SUMMARY_WEEK_COUNT] =
                    weekCount.coerceIn(
                        SummaryHistoryState.DEFAULT_SUMMARY_WEEKS,
                        SummaryHistoryState.MAX_SUMMARY_WEEKS,
                    )
            }
        }

        suspend fun setSummaryFetchedUntil(instant: Instant) {
            context.timesheetDataStore.edit { prefs ->
                prefs[KEY_SUMMARY_FETCHED_UNTIL_MS] = instant.toEpochMilli()
            }
        }

        suspend fun setSummaryOldestSyncedStart(instant: Instant) {
            context.timesheetDataStore.edit { prefs ->
                prefs[KEY_SUMMARY_OLDEST_SYNCED_MS] = instant.toEpochMilli()
            }
        }

        companion object {
            private val KEY_WEEKDAY_HOURS = doublePreferencesKey("weekday_target_hours")
            private val KEY_WORKING_DAYS_MASK = intPreferencesKey("working_days_mask")
            private val KEY_SUMMARY_WEEK_COUNT = intPreferencesKey("summary_week_count")
            private val KEY_SUMMARY_FETCHED_UNTIL_MS = longPreferencesKey("summary_fetched_until_ms")
            private val KEY_SUMMARY_OLDEST_SYNCED_MS = longPreferencesKey("summary_oldest_synced_ms")
        }
    }
