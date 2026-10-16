package dae.aevum.domain.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.proofreaderDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "proofreader_preferences",
)

enum class ProofreaderProvider {
    /** No proofreading. */
    OFF,

    /** Local Ollama server (native chat API). */
    OLLAMA,

    /**
     * OpenAI Chat Completions–compatible HTTP API.
     * Works with OpenAI, Azure OpenAI, Groq, OpenRouter, LM Studio, Ollama `/v1`, etc.
     */
    OPENAI_COMPATIBLE,

    /**
     * On-device proofreading via Gemini Nano (ML Kit GenAI / AICore).
     * Presented as the Koog on-device option: runs entirely on the phone with no API key.
     * (Koog's LiteRT client is still beta and needs bundled models.)
     */
    KOOG_ON_DEVICE,
}

data class ProofreaderConfig(
    val provider: ProofreaderProvider = ProofreaderProvider.OLLAMA,
    val endpointUrl: String = DEFAULT_OLLAMA_URL,
    val model: String = DEFAULT_OLLAMA_MODEL,
    /**
     * Max seconds to wait for the full LLM HTTP response (OkHttp read + call).
     * Connect uses a shorter cap via [connectTimeoutSeconds].
     */
    val connectionTimeoutSeconds: Int = DEFAULT_CONNECTION_TIMEOUT_SECONDS,
    /** Present only in memory when loaded via [ProofreaderPreferences.current]; not stored in DataStore. */
    val apiKey: String = "",
) {
    val enabled: Boolean get() = provider != ProofreaderProvider.OFF

    companion object {
        const val DEFAULT_OLLAMA_URL = "http://192.168.1.9:11434/api/chat"
        const val DEFAULT_OLLAMA_MODEL = "gemma3:4b"
        const val DEFAULT_OPENAI_URL = "https://api.openai.com/v1"
        const val DEFAULT_OPENAI_MODEL = "gpt-4o-mini"
        const val DEFAULT_CONNECTION_TIMEOUT_SECONDS = 60
        /** Local LLMs need tens of seconds; 1s only cancels the call before the model replies. */
        const val MIN_CONNECTION_TIMEOUT_SECONDS = 15
        const val MAX_CONNECTION_TIMEOUT_SECONDS = 600

        /** Cap for TCP connect so a short request timeout still allows a LAN handshake. */
        const val MAX_CONNECT_TIMEOUT_SECONDS = 10

        /** @deprecated Use [DEFAULT_OLLAMA_URL]. */
        const val DEFAULT_ENDPOINT_URL = DEFAULT_OLLAMA_URL

        /** @deprecated Use [DEFAULT_OLLAMA_MODEL]. */
        const val DEFAULT_MODEL = DEFAULT_OLLAMA_MODEL

        fun clampConnectionTimeoutSeconds(seconds: Int): Int =
            seconds.coerceIn(MIN_CONNECTION_TIMEOUT_SECONDS, MAX_CONNECTION_TIMEOUT_SECONDS)

        /** Seconds allowed for establishing the TCP/TLS connection. */
        fun connectTimeoutSeconds(requestTimeoutSeconds: Int): Int =
            clampConnectionTimeoutSeconds(requestTimeoutSeconds)
                .coerceAtMost(MAX_CONNECT_TIMEOUT_SECONDS)

        /** Seconds allowed for waiting on the model response body. */
        fun requestTimeoutSeconds(requestTimeoutSeconds: Int): Int =
            clampConnectionTimeoutSeconds(requestTimeoutSeconds)
    }
}

/**
 * Secure storage for the OpenAI-compatible API key (EncryptedSharedPreferences).
 */
interface ProofreaderSecretStore {
    fun getApiKey(): String?

    fun setApiKey(apiKey: String)

    fun clearApiKey()
}

@Singleton
class ProofreaderPreferences
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val secretStore: ProofreaderSecretStore,
    ) {
        val config: Flow<ProofreaderConfig> =
            context.proofreaderDataStore.data.map { prefs ->
                mapConfig(prefs, secretStore.getApiKey().orEmpty())
            }

        suspend fun current(): ProofreaderConfig {
            val prefs = context.proofreaderDataStore.data.first()
            return mapConfig(prefs, secretStore.getApiKey().orEmpty())
        }

        suspend fun setProvider(provider: ProofreaderProvider) {
            context.proofreaderDataStore.edit { prefs ->
                prefs[KEY_PROVIDER] = provider.name
                // Keep legacy enabled flag in sync for older readers.
                prefs[KEY_ENABLED] = provider != ProofreaderProvider.OFF
            }
        }

        suspend fun setEnabled(enabled: Boolean) {
            setProvider(
                if (enabled) {
                    val current = current().provider
                    if (current == ProofreaderProvider.OFF) ProofreaderProvider.OLLAMA else current
                } else {
                    ProofreaderProvider.OFF
                },
            )
        }

        suspend fun setEndpointUrl(url: String) {
            context.proofreaderDataStore.edit { prefs ->
                prefs[KEY_ENDPOINT_URL] = url.trim()
            }
        }

        suspend fun setModel(model: String) {
            context.proofreaderDataStore.edit { prefs ->
                prefs[KEY_MODEL] = model.trim()
            }
        }

        suspend fun setApiKey(apiKey: String) {
            if (apiKey.isBlank()) {
                secretStore.clearApiKey()
            } else {
                secretStore.setApiKey(apiKey.trim())
            }
            // Touch DataStore so Flow collectors refresh with the new key.
            context.proofreaderDataStore.edit { prefs ->
                prefs[KEY_API_KEY_TOUCH] = System.currentTimeMillis().toString()
            }
        }

        suspend fun setConnectionTimeoutSeconds(seconds: Int) {
            context.proofreaderDataStore.edit { prefs ->
                prefs[KEY_CONNECTION_TIMEOUT_SECONDS] =
                    ProofreaderConfig.clampConnectionTimeoutSeconds(seconds)
            }
        }

        private fun mapConfig(
            prefs: Preferences,
            apiKey: String,
        ): ProofreaderConfig {
            val provider =
                prefs[KEY_PROVIDER]?.let { name ->
                    runCatching { ProofreaderProvider.valueOf(name) }.getOrNull()
                } ?: when (prefs[KEY_ENABLED]) {
                    false -> ProofreaderProvider.OFF
                    else -> ProofreaderProvider.OLLAMA
                }

            val defaults =
                when (provider) {
                    ProofreaderProvider.OPENAI_COMPATIBLE -> {
                        ProofreaderConfig.DEFAULT_OPENAI_URL to ProofreaderConfig.DEFAULT_OPENAI_MODEL
                    }

                    else -> {
                        ProofreaderConfig.DEFAULT_OLLAMA_URL to ProofreaderConfig.DEFAULT_OLLAMA_MODEL
                    }
                }

            return ProofreaderConfig(
                provider = provider,
                endpointUrl = prefs[KEY_ENDPOINT_URL]?.takeIf { it.isNotBlank() } ?: defaults.first,
                model = prefs[KEY_MODEL]?.takeIf { it.isNotBlank() } ?: defaults.second,
                connectionTimeoutSeconds =
                    ProofreaderConfig.clampConnectionTimeoutSeconds(
                        prefs[KEY_CONNECTION_TIMEOUT_SECONDS]
                            ?: ProofreaderConfig.DEFAULT_CONNECTION_TIMEOUT_SECONDS,
                    ),
                apiKey = apiKey,
            )
        }

        companion object {
            private val KEY_ENABLED = booleanPreferencesKey("proofreader_enabled")
            private val KEY_PROVIDER = stringPreferencesKey("proofreader_provider")
            private val KEY_ENDPOINT_URL = stringPreferencesKey("proofreader_endpoint_url")
            private val KEY_MODEL = stringPreferencesKey("proofreader_model")
            private val KEY_CONNECTION_TIMEOUT_SECONDS =
                intPreferencesKey("proofreader_connection_timeout_seconds")
            private val KEY_API_KEY_TOUCH = stringPreferencesKey("proofreader_api_key_touch")
        }
    }
