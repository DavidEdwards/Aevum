package dae.aevum.domain.repositories

/**
 * Runs multiple proofreaders in sequence, feeding the output of one into the next.
 * Stops early if any proofreader throws, returning the last successful text.
 */
class ChainedProofreader(
    private val proofreaders: List<Proofreader>,
) : Proofreader {
    override suspend fun proofread(text: String): String {
        var current = text
        for (engine in proofreaders) {
            try {
                val next = engine.proofread(current)
                if (next.isNotBlank()) {
                    current = next
                }
            } catch (_: Exception) {
                // Ignore and continue with current text
            }
        }
        return current
    }
}
