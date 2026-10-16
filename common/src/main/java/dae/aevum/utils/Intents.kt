package dae.aevum.utils

object Intents {
    const val ACTION_WORKLOG_CHANGED: String = "dae.aevum.action.WORKLOG_CHANGED"
    const val ACTION_TIMER_STOP: String = "dae.aevum.action.TIMER_STOP"
    const val ACTION_TIMER_ADD_15: String = "dae.aevum.action.TIMER_ADD_15"
    const val ACTION_TIMER_SUB_15: String = "dae.aevum.action.TIMER_SUB_15"
    const val ACTION_ASSISTANT_START_TIMER: String = "dae.aevum.action.ASSISTANT_START_TIMER"
    const val ACTION_ASSISTANT_STOP_TIMER: String = "dae.aevum.action.ASSISTANT_STOP_TIMER"

    const val EXTRA_OPEN_STOP_WORKLOG_ID: String = "open_stop_worklog_id"
    const val EXTRA_OPEN_WORKLOG_EDITOR_ID: String = "open_worklog_editor_id"

    /** App Actions GET_THING / in-app search query. */
    const val EXTRA_ASSISTANT_SEARCH_QUERY: String = "assistant_search_query"

    /** App Actions OPEN_APP_FEATURE: issues | worklogs | summary. */
    const val EXTRA_ASSISTANT_OPEN_FEATURE: String = "assistant_open_feature"

    /** App Actions custom START_TIMER issue key (e.g. PROJ-123). */
    const val EXTRA_ASSISTANT_START_ISSUE_KEY: String = "assistant_start_issue_key"

    const val FEATURE_ISSUES: String = "issues"
    const val FEATURE_WORKLOGS: String = "worklogs"
    const val FEATURE_SUMMARY: String = "summary"
}
