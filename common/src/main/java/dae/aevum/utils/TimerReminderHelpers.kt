package dae.aevum.utils

/**
 * Timing helpers for periodic timer-reminder vibrations based on elapsed work time.
 */
object TimerReminderHelpers {
    /**
     * Delay until the next interval milestone after [elapsedMs].
     * Milestones are at interval, 2×interval, 3×interval, …
     */
    fun delayUntilNextMilestoneMs(
        elapsedMs: Long,
        intervalMs: Long,
    ): Long {
        require(intervalMs > 0) { "intervalMs must be > 0" }
        val safeElapsed = elapsedMs.coerceAtLeast(0L)
        val nextMilestoneMs = ((safeElapsed / intervalMs) + 1) * intervalMs
        return (nextMilestoneMs - safeElapsed).coerceAtLeast(1L)
    }
}
