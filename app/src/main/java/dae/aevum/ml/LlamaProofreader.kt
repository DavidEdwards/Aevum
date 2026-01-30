package dae.aevum.ml

import dae.aevum.domain.repositories.Proofreader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Proofreader that sends the text to an Ollama server using the Chat API.
 *
 * Default endpoint assumes a local server at http://192.168.1.9:11434/api/chat
 * and a model name present on the server (e.g., "gemma3:4b" etc.).
 */
class LlamaProofreader(
    private val endpointUrl: String = "http://192.168.1.9:11434/api/chat",
    private val model: String = "gemma3:4b",
    private val httpClient: OkHttpClient =
        OkHttpClient
            .Builder()
            .callTimeout(10, TimeUnit.SECONDS)
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build(),
) : Proofreader {
    override suspend fun proofread(text: String): String =
        withContext(Dispatchers.IO) {
            if (text.isBlank()) return@withContext text

            val systemPrompt = buildSystemPrompt()
            val userContent = text

            // Ollama chat API expects top-level "system" and a messages array of user/assistant turns.
            // Some versions accept a system message inside messages, but others return 400 for that.
            // Use top-level "system" for broad compatibility.
            val json =
                JSONObject()
                    .put("model", model)
                    .put(
                        "messages",
                        JSONArray().put(
                            JSONObject()
                                .put("role", "user")
                                .put("content", systemPrompt + userContent),
                        ),
                    ).put("options", JSONObject().put("temperature", 0.2))
                    .put("stream", false)

            val request =
                Request
                    .Builder()
                    .url(endpointUrl)
                    .addHeader("Accept", "application/json")
                    .post(json.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                    .build()

            try {
                Timber.v("LlamaProofreader: Sending request to %s with model %s", endpointUrl, model)
                httpClient.newCall(request).execute().use { response ->
                    Timber.v("LlamaProofreader: Received response with code %d", response.code)
                    val body = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        val errorPreview = if (body.length > 500) body.take(500) + "..." else body
                        Timber.e(
                            "LlamaProofreader: HTTP %d from server. Body: %s",
                            response.code,
                            errorPreview,
                        )
                        // Try to extract standardized error message
                        try {
                            val err = JSONObject(body).optString("error").takeIf { it.isNotBlank() }
                            if (err != null) {
                                Timber.e("LlamaProofreader: Server error: %s", err)
                            }
                        } catch (_: Throwable) {
                            // ignore JSON error parsing
                        }
                        return@use
                    }
                    Timber.v("LlamaProofreader: Response body length %d", body.length)
                    if (body.isBlank()) return@use

                    val root = JSONObject(body)
                    val message = root.optJSONObject("message")
                    val content = message?.optString("content")?.trim()
                    Timber.v("LlamaProofreader: Extracted content: %s", content)
                    if (!content.isNullOrBlank()) {
                        return@withContext finalizeOutput(
                            text,
                            sanitizeOutput(content),
                        )
                    }
                    // Fallback to generate API shape if chat isn't available
                    val genResponse = root.optString("response").trim()
                    if (genResponse.isNotBlank()) {
                        return@withContext finalizeOutput(
                            text,
                            sanitizeOutput(genResponse),
                        )
                    }
                }
            } catch (e: Throwable) {
                Timber.e(e, "LlamaProofreader: Error during proofreading, returning original text")
                // Swallow and fall back to original text
            }

            text
        }

    private fun buildSystemPrompt(): String =
        """
        You are an expert copy editor for software engineering worklog summaries.
        Task: Proofread the user's summary. Fix grammar, punctuation, capitalization, and small word choices.
        Constraints:
        - Preserve meaning exactly; do not add or remove information.
        - Keep Jira issue keys (e.g., ABC-123), code identifiers, filenames, commands, times, and numbers unchanged.
        - Retain domain-specific terms and acronyms.
        - Maintain the original tense and voice where reasonable.
        - Keep it concise and professional.
        Output requirements (STRICT):
        - Return ONLY the corrected summary text as a single line.
        - Do not add greetings, explanations, bullet points, or emojis.
        - Do not wrap the text in quotes.
        - If the text is already acceptable, return it unchanged.
        If you cannot comply exactly, return the input unchanged.
        
        Proof the following text:
        
        """.trimIndent()

    private fun sanitizeOutput(modelOutput: String): String {
        // Remove surrounding quotes if present and trim whitespace
        var out = modelOutput.trim()
        if (out.length >= 2 && (
                (out.startsWith('"') && out.endsWith('"')) || (
                    out.startsWith('\'') &&
                        out.endsWith(
                            '\'',
                        )
                )
            )
        ) {
            out = out.substring(1, out.length - 1).trim()
        }
        // Normalize line endings and collapse excessive whitespace
        out = out.replace("\r\n", "\n").replace("\r", "\n").trim()
        // If multiple paragraphs, prefer the first non-empty line as the corrected summary
        val firstNonEmptyLine = out.lines().firstOrNull { it.isNotBlank() }?.trim()
        return firstNonEmptyLine ?: out
    }

    private fun finalizeOutput(
        original: String,
        candidate: String,
    ): String {
        val trimmedOriginal = original.trim()
        var trimmedCandidate = candidate.trim()

        // Reject empty
        if (trimmedCandidate.isBlank()) return trimmedOriginal

        // Collapse to single line if needed (keep first non-empty line)
        if (trimmedCandidate.contains('\n')) {
            trimmedCandidate = trimmedCandidate.lines().firstOrNull { it.isNotBlank() }?.trim()
                ?: trimmedCandidate.replace("\n", " ").trim()
        }

        // Reject obviously chatty prefixes, bullets, or emojis
        val chattyPrefixes =
            Regex("^(?i)(okay|sure|here|i'm|i am|i can|do you want|let me|great|awesome|sounds good)[^a-zA-Z0-9]*")
        val hasBullets = trimmedCandidate.startsWith("-") || trimmedCandidate.startsWith("*")
        val hasEmojis =
            trimmedCandidate.any { Character.getType(it) == Character.OTHER_SYMBOL.toInt() }

        val shouldFallback =
            chattyPrefixes.containsMatchIn(trimmedCandidate) || hasBullets || hasEmojis
        if (shouldFallback) {
            Timber.v("LlamaProofreader: Discarding candidate due to heuristics; using original")
            return trimmedOriginal
        }

        Timber.v("LlamaProofreader: Using refined candidate: %s", trimmedCandidate)
        return trimmedCandidate
    }
}
