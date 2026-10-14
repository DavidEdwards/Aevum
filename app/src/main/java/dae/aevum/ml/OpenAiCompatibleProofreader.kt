package dae.aevum.ml

import dae.aevum.common.R
import dae.aevum.domain.preferences.LocalNetworkPermissionGate
import dae.aevum.domain.preferences.ProofreaderConfig
import dae.aevum.domain.preferences.ProofreaderProgress
import dae.aevum.utils.LocalNetworkUrls
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Proofreader for any OpenAI Chat Completions–compatible endpoint
 * (OpenAI, Azure OpenAI, Groq, OpenRouter, LM Studio, Ollama `/v1`, etc.).
 *
 * [ProofreaderConfig.endpointUrl] should be the API root (e.g. `https://api.openai.com/v1`);
 * requests go to `{url}/chat/completions` with a Bearer token.
 */
@Singleton
class OpenAiCompatibleProofreader
    @Inject
    constructor(
        private val localNetworkPermissionGate: LocalNetworkPermissionGate,
        private val progress: ProofreaderProgress,
    ) {
        private val baseHttpClient = OkHttpClient()

        suspend fun proofread(
            text: String,
            config: ProofreaderConfig,
        ): String =
            withContext(Dispatchers.IO) {
                if (text.isBlank()) return@withContext text
                if (config.apiKey.isBlank()) {
                    Timber.w("OpenAiCompatibleProofreader: Missing API key; returning original text")
                    progress.log(R.string.proofreader_log_missing_key)
                    return@withContext text
                }

                val httpClient = clientFor(config.connectionTimeoutSeconds)
                val url = chatCompletionsUrl(config.endpointUrl)
                progress.log(R.string.proofreader_log_model, config.model)
                progress.log(R.string.proofreader_log_connecting, url)
                val json =
                    JSONObject()
                        .put("model", config.model)
                        .put(
                            "messages",
                            JSONArray()
                                .put(
                                    JSONObject()
                                        .put("role", "system")
                                        .put("content", ProofreadTextHelpers.systemPrompt()),
                                ).put(
                                    JSONObject()
                                        .put("role", "user")
                                        .put("content", text),
                                ),
                        ).put("temperature", 0.2)

                val request =
                    Request
                        .Builder()
                        .url(url)
                        .addHeader("Accept", "application/json")
                        .addHeader("Authorization", "Bearer ${config.apiKey}")
                        .post(json.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                        .build()

                try {
                    progress.log(R.string.proofreader_log_waiting)
                    Timber.v(
                        "OpenAiCompatibleProofreader: POST %s model=%s",
                        url,
                        config.model,
                    )
                    httpClient.newCall(request).execute().use { response ->
                        val body = response.body?.string().orEmpty()
                        if (!response.isSuccessful) {
                            Timber.e(
                                "OpenAiCompatibleProofreader: HTTP %d. Body: %s",
                                response.code,
                                body.take(500),
                            )
                            progress.log(R.string.proofreader_log_http_error, response.code)
                            return@withContext text
                        }
                        progress.log(R.string.proofreader_log_received)
                        val content =
                            JSONObject(body)
                                .optJSONArray("choices")
                                ?.optJSONObject(0)
                                ?.optJSONObject("message")
                                ?.optString("content")
                                ?.trim()
                        if (content.isNullOrBlank()) {
                            Timber.w("OpenAiCompatibleProofreader: Empty content in response")
                            progress.log(R.string.proofreader_log_empty_response)
                            return@withContext text
                        }
                        progress.log(R.string.proofreader_log_applying)
                        val refined =
                            ProofreadTextHelpers.finalizeOutput(
                                text,
                                ProofreadTextHelpers.sanitizeOutput(content),
                            )
                        progress.log(R.string.proofreader_log_done)
                        return@withContext refined
                    }
                } catch (e: Throwable) {
                    maybeSignalLocalNetworkPermission(e, config.endpointUrl)
                    progress.log(R.string.proofreader_log_connect_failed)
                    Timber.e(e, "OpenAiCompatibleProofreader: Error; returning original text")
                    text
                }
            }

        private fun clientFor(timeoutSeconds: Int): OkHttpClient {
            val seconds =
                ProofreaderConfig
                    .clampConnectionTimeoutSeconds(timeoutSeconds)
                    .toLong()
            return baseHttpClient
                .newBuilder()
                .callTimeout(seconds, TimeUnit.SECONDS)
                .connectTimeout(seconds, TimeUnit.SECONDS)
                .readTimeout(seconds, TimeUnit.SECONDS)
                .build()
        }

        private fun maybeSignalLocalNetworkPermission(
            error: Throwable,
            endpointUrl: String,
        ) {
            val connectFailed =
                error is SocketTimeoutException ||
                    error is ConnectException ||
                    error.cause is SocketTimeoutException ||
                    error.cause is ConnectException
            if (connectFailed && LocalNetworkUrls.looksLikeLocalNetworkUrl(endpointUrl)) {
                Timber.i("OpenAiCompatibleProofreader: LAN connect failed; requesting local network permission")
                localNetworkPermissionGate.notifyAccessRequired()
            }
        }

        companion object {
            fun chatCompletionsUrl(endpointUrl: String): String {
                val trimmed = endpointUrl.trim().trimEnd('/')
                return when {
                    trimmed.endsWith("/chat/completions") -> trimmed
                    trimmed.endsWith("/v1") -> "$trimmed/chat/completions"
                    else -> "$trimmed/v1/chat/completions"
                }
            }
        }
    }
