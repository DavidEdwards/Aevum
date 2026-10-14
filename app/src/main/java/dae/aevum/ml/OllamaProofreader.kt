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
 * Proofreader for a local/network Ollama server using the native Chat API
 * (`/api/chat`). Prefer [OpenAiCompatibleProofreader] if the server exposes `/v1`.
 */
@Singleton
class OllamaProofreader
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

                val httpClient = clientFor(config.connectionTimeoutSeconds)
                val endpointUrl = normalizeChatUrl(config.endpointUrl)
                progress.log(R.string.proofreader_log_model, config.model)
                progress.log(R.string.proofreader_log_connecting, endpointUrl)
                val json =
                    JSONObject()
                        .put("model", config.model)
                        .put(
                            "messages",
                            JSONArray().put(
                                JSONObject()
                                    .put("role", "user")
                                    .put("content", ProofreadTextHelpers.systemPrompt() + "\n\n" + text),
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
                    progress.log(R.string.proofreader_log_waiting)
                    Timber.v("OllamaProofreader: POST %s model=%s", endpointUrl, config.model)
                    httpClient.newCall(request).execute().use { response ->
                        val body = response.body?.string().orEmpty()
                        if (!response.isSuccessful) {
                            Timber.e(
                                "OllamaProofreader: HTTP %d. Body: %s",
                                response.code,
                                body.take(500),
                            )
                            progress.log(R.string.proofreader_log_http_error, response.code)
                            return@withContext text
                        }
                        if (body.isBlank()) {
                            progress.log(R.string.proofreader_log_empty_response)
                            return@withContext text
                        }

                        progress.log(R.string.proofreader_log_received)
                        val root = JSONObject(body)
                        val content =
                            root.optJSONObject("message")?.optString("content")?.trim()
                                ?: root.optString("response").trim().takeIf { it.isNotBlank() }
                        if (content.isNullOrBlank()) {
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
                    Timber.e(e, "OllamaProofreader: Error; returning original text")
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
                Timber.i("OllamaProofreader: LAN connect failed; requesting local network permission")
                localNetworkPermissionGate.notifyAccessRequired()
            }
        }

        companion object {
            fun normalizeChatUrl(endpointUrl: String): String {
                val trimmed = endpointUrl.trim().trimEnd('/')
                return when {
                    trimmed.endsWith("/api/chat") -> trimmed
                    trimmed.endsWith("/api") -> "$trimmed/chat"
                    else -> "$trimmed/api/chat"
                }
            }
        }
    }
