package dae.aevum.utils

/**
 * Builds Jira JQL for the in-app issue search field.
 *
 * Numeric terms are treated as either an internal issue id or a key suffix
 * (e.g. "5712" → `id = 5712` and `PROJ-5712` for known projects).
 */
object IssueSearchQuery {
    private val FULL_ISSUE_KEY = Regex("^[A-Za-z][A-Za-z0-9_]+-\\d+$")

    fun isFullIssueKey(term: String): Boolean = FULL_ISSUE_KEY.matches(term.trim())

    fun isNumericId(term: String): Boolean {
        val trimmed = term.trim()
        return trimmed.isNotEmpty() && trimmed.all { it.isDigit() }
    }

    fun projectKeyFromIssueId(issueId: String): String? {
        val dash = issueId.indexOf('-')
        if (dash <= 0) return null
        return issueId.substring(0, dash).takeIf { it.isNotBlank() }
    }

    fun escapeJqlString(term: String): String =
        term
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")

    fun buildJql(
        term: String,
        knownProjectKeys: Collection<String> = emptyList(),
    ): String {
        val trimmed = term.trim()
        require(trimmed.isNotEmpty()) { "search term must not be blank" }
        val escaped = escapeJqlString(trimmed)

        return when {
            isFullIssueKey(trimmed) -> {
                "key = $trimmed"
            }

            isNumericId(trimmed) -> {
                val keyCandidates =
                    knownProjectKeys
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .distinct()
                        .map { "$it-$trimmed" }
                buildString {
                    append("id = $trimmed")
                    if (keyCandidates.isNotEmpty()) {
                        append(" OR key in (${keyCandidates.joinToString(",")})")
                    }
                    append(" OR text ~ \"$escaped\" OR summary ~ \"$escaped\" ORDER BY updated DESC")
                }
            }

            else -> {
                "text ~ \"$escaped\" OR summary ~ \"$escaped\" ORDER BY updated DESC"
            }
        }
    }
}
