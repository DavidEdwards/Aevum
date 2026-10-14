package dae.aevum.ml

import dae.aevum.common.R
import dae.aevum.domain.preferences.ProofreaderPreferences
import dae.aevum.domain.preferences.ProofreaderProgress
import dae.aevum.domain.preferences.ProofreaderProvider
import dae.aevum.domain.repositories.Proofreader
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Routes proofreading to Ollama, an OpenAI-compatible HTTP API, or on-device ML Kit
 * based on [ProofreaderPreferences].
 */
@Singleton
class ConfigurableProofreader
    @Inject
    constructor(
        private val preferences: ProofreaderPreferences,
        private val progress: ProofreaderProgress,
        private val ollama: OllamaProofreader,
        private val openAiCompatible: OpenAiCompatibleProofreader,
        private val onDevice: MlKitProofreader,
    ) : Proofreader {
        override suspend fun proofread(text: String): String {
            progress.clear()
            progress.log(R.string.proofreader_log_starting)
            if (text.isBlank()) {
                progress.log(R.string.proofreader_log_skipped_blank)
                return text
            }
            val config = preferences.current()
            return when (config.provider) {
                ProofreaderProvider.OFF -> {
                    Timber.v("ConfigurableProofreader: Off")
                    progress.log(R.string.proofreader_log_off)
                    text
                }

                ProofreaderProvider.OLLAMA -> {
                    Timber.v("ConfigurableProofreader: Ollama")
                    progress.log(R.string.proofreader_log_provider_ollama)
                    ollama.proofread(text, config)
                }

                ProofreaderProvider.OPENAI_COMPATIBLE -> {
                    Timber.v("ConfigurableProofreader: OpenAI-compatible")
                    progress.log(R.string.proofreader_log_provider_openai)
                    openAiCompatible.proofread(text, config)
                }

                ProofreaderProvider.KOOG_ON_DEVICE -> {
                    Timber.v("ConfigurableProofreader: Koog on-device (ML Kit / Gemini Nano)")
                    progress.log(R.string.proofreader_log_provider_on_device)
                    onDevice.proofread(text)
                }
            }
        }
    }
