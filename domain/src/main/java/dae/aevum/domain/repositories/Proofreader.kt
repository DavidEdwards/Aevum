package dae.aevum.domain.repositories

/**
 * Abstraction for AI proofreading of user-provided text.
 * Implementations may use on-device ML (e.g., ML Kit GenAI Proofreading) or be a no-op.
 */
interface Proofreader {
    /**
     * Returns a refined version of the provided [text]. Implementations should be fast and safe.
     * If the [text] is blank, they should return it unchanged.
     */
    suspend fun proofread(text: String): String
}
