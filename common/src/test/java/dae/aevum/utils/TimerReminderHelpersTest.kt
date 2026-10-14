package dae.aevum.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class TimerReminderHelpersTest {
    @Test
    fun delayUntilNextMilestone_fromStart() {
        assertEquals(
            15L * 60_000L,
            TimerReminderHelpers.delayUntilNextMilestoneMs(0L, 15L * 60_000L),
        )
    }

    @Test
    fun delayUntilNextMilestone_midInterval() {
        val interval = 15L * 60_000L
        assertEquals(
            5L * 60_000L,
            TimerReminderHelpers.delayUntilNextMilestoneMs(10L * 60_000L, interval),
        )
    }

    @Test
    fun delayUntilNextMilestone_exactlyOnMilestone_waitsFullInterval() {
        val interval = 15L * 60_000L
        assertEquals(
            interval,
            TimerReminderHelpers.delayUntilNextMilestoneMs(interval, interval),
        )
    }
}
