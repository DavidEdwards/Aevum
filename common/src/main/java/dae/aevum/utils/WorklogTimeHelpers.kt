package dae.aevum.utils

import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/**
 * Shared helpers for timesheet day windows, live active duration, and daily targets.
 */
object WorklogTimeHelpers {
    const val WEEKDAY_TARGET_HOURS = 8.0
    const val WEEKEND_TARGET_HOURS = 0.0
    const val WEEK_TARGET_HOURS = 40.0

    fun zone(): ZoneId = ZoneId.systemDefault()

    /** Start of [date] in the system default zone as an Instant. */
    fun startOfDay(date: LocalDate, zone: ZoneId = zone()): Instant =
        date.atStartOfDay(zone).toInstant()

    /** Exclusive end of [date] (start of next day) in the system default zone. */
    fun endOfDayExclusive(date: LocalDate, zone: ZoneId = zone()): Instant =
        date.plusDays(1).atStartOfDay(zone).toInstant()

    /** Inclusive local-day window for "today". */
    fun todayWindow(now: LocalDate = LocalDate.now(zone()), zone: ZoneId = zone()): Pair<Instant, Instant> =
        startOfDay(now, zone) to endOfDayExclusive(now, zone)

    /**
     * Last 14 local calendar days ending at end of today (exclusive end = start of tomorrow).
     * Range covers [today - 13 days, tomorrow).
     */
    fun lastTwoWeeksWindow(
        now: LocalDate = LocalDate.now(zone()),
        zone: ZoneId = zone(),
    ): Pair<Instant, Instant> {
        val end = endOfDayExclusive(now, zone)
        val start = startOfDay(now.minusDays(13), zone)
        return start to end
    }

    /** Monday 00:00 through Sunday end (exclusive Monday next week) of the week containing [date]. */
    fun currentWeekWindow(
        date: LocalDate = LocalDate.now(zone()),
        zone: ZoneId = zone(),
    ): Pair<Instant, Instant> {
        val monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val nextMonday = monday.plusWeeks(1)
        return startOfDay(monday, zone) to startOfDay(nextMonday, zone)
    }

    fun dayTargetHours(
        day: LocalDate,
        weekdayTargetHours: Double = WEEKDAY_TARGET_HOURS,
    ): Double =
        if (day.dayOfWeek.value in 1..5) weekdayTargetHours else WEEKEND_TARGET_HOURS

    /**
     * Whether logged hours meet the day target (weekday target, weekend 0h).
     * Weekends with catch-up hours still meet the 0h target (hours >= 0).
     */
    fun meetsDayTarget(
        hours: Double,
        day: LocalDate,
        weekdayTargetHours: Double = WEEKDAY_TARGET_HOURS,
    ): Boolean = hours >= dayTargetHours(day, weekdayTargetHours)

    /**
     * Bar coloring: weekdays use weekday threshold; weekends use 0h so under-target is not marked bad.
     * Catch-up hours on weekends still count as meeting the day target.
     */
    fun isDayOnTrack(
        hours: Float,
        day: LocalDate,
        weekdayTargetHours: Double = WEEKDAY_TARGET_HOURS,
    ): Boolean = hours >= dayTargetHours(day, weekdayTargetHours).toFloat()

    /** Progress fraction for a bar; target 0 avoids divide-by-zero (weekend catch-up). */
    fun progressFraction(
        hours: Double,
        target: Double,
    ): Float =
        when {
            target <= 0.0 -> if (hours > 0.0) 1f else 0f
            else -> (hours / target).toFloat().coerceIn(0f, 1f)
        }

    fun overtimeHours(
        hours: Double,
        target: Double,
    ): Double = (hours - target).coerceAtLeast(0.0)

    fun effectiveTo(from: Instant, to: Instant, active: Boolean, now: Instant): Instant =
        if (active) now else to

    fun durationSeconds(from: Instant, to: Instant, active: Boolean, now: Instant): Long {
        val end = effectiveTo(from, to, active, now)
        return Duration.between(from, end).seconds.coerceAtLeast(0)
    }

    fun hours(from: Instant, to: Instant, active: Boolean, now: Instant): Double =
        durationSeconds(from, to, active, now) / 3600.0

    fun totalHours(
        entries: List<TimedEntry>,
        now: Instant,
    ): Double = entries.sumOf { hours(it.from, it.to, it.active, now) }

    /**
     * True overlap: interval [from, to] overlaps [rangeStart, rangeEnd]
     * when from < rangeEnd AND to > rangeStart (using exclusive end semantics for day windows).
     */
    fun overlaps(
        from: Instant,
        to: Instant,
        rangeStart: Instant,
        rangeEnd: Instant,
    ): Boolean = from < rangeEnd && to > rangeStart

    data class TimedEntry(
        val from: Instant,
        val to: Instant,
        val active: Boolean,
    )
}
