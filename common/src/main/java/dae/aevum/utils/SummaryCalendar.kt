package dae.aevum.utils

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Rows rendered by the summary week calendar (newest weeks first). */
sealed class SummaryCalendarItem {
    data class MonthHeader(
        val yearMonth: YearMonth,
        val label: String,
    ) : SummaryCalendarItem()

    data class WeekRow(
        val weekStart: LocalDate,
        val days: List<LocalDate>,
    ) : SummaryCalendarItem()
}

object SummaryCalendar {
    private val monthYearFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())

    /**
     * Builds calendar list items for [weekCount] weeks ending with the week that contains [today],
     * newest first. Inserts a month/year heading whenever the week's Monday enters a new month.
     */
    fun buildItems(
        weekCount: Int,
        today: LocalDate = LocalDate.now(),
    ): List<SummaryCalendarItem> {
        val weeks = weekCount.coerceAtLeast(1)
        val thisMonday = WorklogTimeHelpers.startOfWeekMonday(today)
        val items = mutableListOf<SummaryCalendarItem>()
        var previousMonth: YearMonth? = null

        for (i in 0 until weeks) {
            val monday = thisMonday.minusWeeks(i.toLong())
            val yearMonth = YearMonth.from(monday)
            if (yearMonth != previousMonth) {
                items.add(
                    SummaryCalendarItem.MonthHeader(
                        yearMonth = yearMonth,
                        label = yearMonth.atDay(1).format(monthYearFormatter),
                    ),
                )
                previousMonth = yearMonth
            }
            items.add(
                SummaryCalendarItem.WeekRow(
                    weekStart = monday,
                    days = (0..6).map { monday.plusDays(it.toLong()) },
                ),
            )
        }
        return items
    }
}
