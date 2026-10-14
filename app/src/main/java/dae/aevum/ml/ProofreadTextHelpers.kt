package dae.aevum.ml

import timber.log.Timber

/**
 * Shared prompt + output sanitization for LLM-based worklog proofreading.
 */
internal object ProofreadTextHelpers {
    fun systemPrompt(): String =
        """
        You are an expert copy editor for software engineering worklog summaries.
        Task: Proofread the user's summary. Fix grammar, punctuation, capitalization, and small word choices.
        Constraints:
        - Preserve meaning exactly; do not add or remove information.
        - Sentences begin capitalized and end with a `.`.
        - Keep Jira issue keys (e.g., ABC-123), code identifiers, filenames, commands, times, and numbers unchanged.
        - Retain domain-specific terms and acronyms.
        - Maintain the original tense and voice where reasonable.
        - Keep it concise and professional.
        Output requirements (STRICT):
        - Return ONLY the corrected summary text.
        - Do not add greetings, explanations, bullet points, or emojis.
        - Do not wrap the text in quotes.
        If you cannot comply exactly, return the input unchanged.
        """.trimIndent()

    fun sanitizeOutput(modelOutput: String): String {
        var out = modelOutput.trim()
        if (out.length >= 2 && (
                (out.startsWith('"') && out.endsWith('"')) ||
                    (out.startsWith('\'') && out.endsWith('\''))
            )
        ) {
            out = out.substring(1, out.length - 1).trim()
        }
        out = out.replace("\r\n", "\n").replace("\r", "\n").trim()
        return out.lines().firstOrNull { it.isNotBlank() }?.trim() ?: out
    }

    fun finalizeOutput(
        original: String,
        candidate: String,
    ): String {
        val trimmedOriginal = original.trim()
        var trimmedCandidate = candidate.trim()

        if (trimmedCandidate.isBlank()) return trimmedOriginal

        if (trimmedCandidate.contains('\n')) {
            trimmedCandidate = trimmedCandidate.lines().firstOrNull { it.isNotBlank() }?.trim()
                ?: trimmedCandidate.replace("\n", " ").trim()
        }

        val chattyPrefixes =
            Regex("^(?i)(okay|sure|here|i'm|i am|i can|do you want|let me|great|awesome|sounds good)[^a-zA-Z0-9]*")
        val hasBullets = trimmedCandidate.startsWith("-") || trimmedCandidate.startsWith("*")
        val hasEmojis =
            trimmedCandidate.any { Character.getType(it) == Character.OTHER_SYMBOL.toInt() }

        if (chattyPrefixes.containsMatchIn(trimmedCandidate) || hasBullets || hasEmojis) {
            Timber.v("ProofreadTextHelpers: Discarding candidate due to heuristics; using original")
            return trimmedOriginal
        }

        Timber.v("ProofreadTextHelpers: Using refined candidate: %s", trimmedCandidate)
        return trimmedCandidate
    }
}
