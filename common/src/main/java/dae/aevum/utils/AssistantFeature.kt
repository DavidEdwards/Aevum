package dae.aevum.utils

/**
 * Normalizes App Actions OPEN_APP_FEATURE values (shortcutId or spoken synonym)
 * to a canonical route key used by [Intents.EXTRA_ASSISTANT_OPEN_FEATURE].
 */
object AssistantFeature {
    fun normalize(raw: String?): String? {
        val value = raw?.trim()?.lowercase() ?: return null
        if (value.isEmpty()) return null
        return when (value) {
            Intents.FEATURE_ISSUES,
            "issue list",
            "tickets",
            "jira issues",
            -> Intents.FEATURE_ISSUES

            Intents.FEATURE_WORKLOGS,
            "pending",
            "pending worklogs",
            "upload queue",
            -> Intents.FEATURE_WORKLOGS

            Intents.FEATURE_SUMMARY,
            "timesheet",
            "worklog summary",
            "time summary",
            -> Intents.FEATURE_SUMMARY

            else -> value
        }
    }

    fun isKnown(feature: String?): Boolean =
        when (normalize(feature)) {
            Intents.FEATURE_ISSUES,
            Intents.FEATURE_WORKLOGS,
            Intents.FEATURE_SUMMARY,
            -> true
            else -> false
        }
}
