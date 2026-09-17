package dae.aevum.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class WorklogTimeHelpersTest {
    private val zurich = ZoneId.of("Europe/Zurich")

    @Test
    fun todayWindow_usesLocalZoneNotUtc() {
        val date = LocalDate.of(2026, 3, 15)
        val (start, end) = WorklogTimeHelpers.todayWindow(date, zurich)

        assertEquals(date.atStartOfDay(zurich).toInstant(), start)
        assertEquals(date.plusDays(1).atStartOfDay(zurich).toInstant(), end)
        // Zurich is UTC+1 in March (CET) — start must not equal UTC midnight
        assertFalse(start == date.atStartOfDay(ZoneOffset.UTC).toInstant())
    }

    @Test
    fun lastTwoWeeksWindow_coversFourteenLocalDays() {
        val date = LocalDate.of(2026, 9, 17)
        val (start, end) = WorklogTimeHelpers.lastTwoWeeksWindow(date, zurich)
        assertEquals(LocalDate.of(2026, 9, 4).atStartOfDay(zurich).toInstant(), start)
        assertEquals(LocalDate.of(2026, 9, 18).atStartOfDay(zurich).toInstant(), end)
    }

    @Test
    fun currentWeekWindow_mondayToNextMonday() {
        val wednesday = LocalDate.of(2026, 9, 16)
        val (start, end) = WorklogTimeHelpers.currentWeekWindow(wednesday, zurich)
        assertEquals(LocalDate.of(2026, 9, 14).atStartOfDay(zurich).toInstant(), start)
        assertEquals(LocalDate.of(2026, 9, 21).atStartOfDay(zurich).toInstant(), end)
    }

    @Test
    fun dayTargetHours_weekdaysEightWeekendsZero() {
        assertEquals(8.0, WorklogTimeHelpers.dayTargetHours(LocalDate.of(2026, 9, 14)), 0.0) // Mon
        assertEquals(8.0, WorklogTimeHelpers.dayTargetHours(LocalDate.of(2026, 9, 18)), 0.0) // Fri
        assertEquals(0.0, WorklogTimeHelpers.dayTargetHours(LocalDate.of(2026, 9, 19)), 0.0) // Sat
        assertEquals(0.0, WorklogTimeHelpers.dayTargetHours(LocalDate.of(2026, 9, 20)), 0.0) // Sun
    }

    @Test
    fun meetsDayTarget_weekendZeroHoursMeets_weekdayNeedsEight() {
        val saturday = LocalDate.of(2026, 9, 20)
        val monday = LocalDate.of(2026, 9, 14)
        assertTrue(WorklogTimeHelpers.meetsDayTarget(0.0, saturday))
        assertTrue(WorklogTimeHelpers.meetsDayTarget(2.5, saturday))
        assertFalse(WorklogTimeHelpers.meetsDayTarget(7.9, monday))
        assertTrue(WorklogTimeHelpers.meetsDayTarget(8.0, monday))
    }

    @Test
    fun dayTargetHours_customWeekdayTarget() {
        assertEquals(6.0, WorklogTimeHelpers.dayTargetHours(LocalDate.of(2026, 9, 14), 6.0), 0.0)
        assertEquals(0.0, WorklogTimeHelpers.dayTargetHours(LocalDate.of(2026, 9, 19), 6.0), 0.0)
    }

    @Test
    fun progressFraction_handlesZeroTargetAndOvertimeCap() {
        assertEquals(0f, WorklogTimeHelpers.progressFraction(0.0, 0.0), 0.001f)
        assertEquals(1f, WorklogTimeHelpers.progressFraction(2.0, 0.0), 0.001f)
        assertEquals(0.5f, WorklogTimeHelpers.progressFraction(4.0, 8.0), 0.001f)
        assertEquals(1f, WorklogTimeHelpers.progressFraction(10.0, 8.0), 0.001f)
    }

    @Test
    fun overtimeHours_returnsSurplusOnly() {
        assertEquals(0.0, WorklogTimeHelpers.overtimeHours(7.0, 8.0), 0.001)
        assertEquals(1.5, WorklogTimeHelpers.overtimeHours(9.5, 8.0), 0.001)
        assertEquals(3.0, WorklogTimeHelpers.overtimeHours(3.0, 0.0), 0.001)
    }

    @Test
    fun effectiveTo_usesNowWhenActive() {
        val from = Instant.parse("2026-09-17T08:00:00Z")
        val to = Instant.parse("2026-09-17T08:00:00Z")
        val now = Instant.parse("2026-09-17T10:30:00Z")
        assertEquals(now, WorklogTimeHelpers.effectiveTo(from, to, active = true, now = now))
        assertEquals(to, WorklogTimeHelpers.effectiveTo(from, to, active = false, now = now))
    }

    @Test
    fun hours_activeTimerUsesNow() {
        val from = Instant.parse("2026-09-17T08:00:00Z")
        val to = Instant.parse("2026-09-17T08:00:00Z")
        val now = Instant.parse("2026-09-17T10:00:00Z")
        assertEquals(2.0, WorklogTimeHelpers.hours(from, to, active = true, now = now), 0.001)
    }

    @Test
    fun overlaps_trueOverlapIncludingFullySpanning() {
        val rangeStart = Instant.parse("2026-09-10T00:00:00Z")
        val rangeEnd = Instant.parse("2026-09-17T00:00:00Z")
        // Fully spans range
        assertTrue(
            WorklogTimeHelpers.overlaps(
                Instant.parse("2026-09-01T00:00:00Z"),
                Instant.parse("2026-09-20T00:00:00Z"),
                rangeStart,
                rangeEnd,
            ),
        )
        // Outside
        assertFalse(
            WorklogTimeHelpers.overlaps(
                Instant.parse("2026-09-17T00:00:00Z"),
                Instant.parse("2026-09-18T00:00:00Z"),
                rangeStart,
                rangeEnd,
            ),
        )
    }
}
