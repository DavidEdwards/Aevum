package dae.aevum.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

class SummaryCalendarTest {
    @Test
    fun buildItems_newestWeekFirst_mondayStart() {
        // Wednesday 2026-04-08 → week starts Monday 2026-04-06
        val today = LocalDate.of(2026, 4, 8)
        val items = SummaryCalendar.buildItems(weekCount = 2, today = today)

        val weeks = items.filterIsInstance<SummaryCalendarItem.WeekRow>()
        assertEquals(2, weeks.size)
        assertEquals(LocalDate.of(2026, 4, 6), weeks[0].weekStart)
        assertEquals(LocalDate.of(2026, 3, 30), weeks[1].weekStart)
        assertEquals(DayOfWeek.MONDAY, weeks[0].days.first().dayOfWeek)
        assertEquals(DayOfWeek.SUNDAY, weeks[0].days.last().dayOfWeek)
    }

    @Test
    fun buildItems_insertsMonthHeaderWhenMondayMonthChanges() {
        val today = LocalDate.of(2026, 4, 8)
        val items = SummaryCalendar.buildItems(weekCount = 3, today = today)

        val headers = items.filterIsInstance<SummaryCalendarItem.MonthHeader>()
        assertTrue(headers.any { it.yearMonth == YearMonth.of(2026, 4) })
        assertTrue(headers.any { it.yearMonth == YearMonth.of(2026, 3) })

        // April header should appear before the April week; March before March weeks.
        val aprilIndex = items.indexOfFirst { it is SummaryCalendarItem.MonthHeader && it.yearMonth.monthValue == 4 }
        val firstAprilWeek =
            items.indexOfFirst {
                it is SummaryCalendarItem.WeekRow && it.weekStart.monthValue == 4
            }
        assertTrue(aprilIndex < firstAprilWeek)
    }

    @Test
    fun dayHoursStatus_classifiesMissingAndOver() {
        val monday = LocalDate.of(2026, 4, 6)
        assertEquals(
            WorklogTimeHelpers.DayHoursStatus.UNDER,
            WorklogTimeHelpers.dayHoursStatus(4.0, monday, today = monday),
        )
        assertEquals(
            WorklogTimeHelpers.DayHoursStatus.MET,
            WorklogTimeHelpers.dayHoursStatus(8.0, monday, today = monday),
        )
        assertEquals(
            WorklogTimeHelpers.DayHoursStatus.OVER,
            WorklogTimeHelpers.dayHoursStatus(11.0, monday, today = monday),
        )
        assertEquals(
            WorklogTimeHelpers.DayHoursStatus.FUTURE,
            WorklogTimeHelpers.dayHoursStatus(0.0, monday.plusDays(1), today = monday),
        )
        assertEquals(
            WorklogTimeHelpers.DayHoursStatus.EMPTY_WEEKEND,
            WorklogTimeHelpers.dayHoursStatus(0.0, monday.plusDays(5), today = monday.plusDays(5)),
        )
    }
}
