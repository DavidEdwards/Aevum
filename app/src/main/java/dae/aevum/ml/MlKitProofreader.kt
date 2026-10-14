package dae.aevum.ml

import android.content.Context
import com.google.mlkit.genai.common.DownloadCallback
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.common.GenAiException
import com.google.mlkit.genai.proofreading.ProofreaderOptions
import com.google.mlkit.genai.proofreading.Proofreading
import com.google.mlkit.genai.proofreading.ProofreadingRequest
import dae.aevum.common.R
import dae.aevum.domain.preferences.ProofreaderProgress
import dae.aevum.domain.repositories.Proofreader
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import com.google.mlkit.genai.proofreading.Proofreader as MlProofreaderClient

/**
 * On-device proofreading via ML Kit GenAI (Gemini Nano through AICore).
 * This is the practical Android on-device path while Koog LiteRT still requires
 * bundled model files (see https://docs.koog.ai/).
 */
@Singleton
class MlKitProofreader
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val progress: ProofreaderProgress,
    ) : Proofreader {
        override suspend fun proofread(text: String): String =
            withContext(Dispatchers.IO) {
                if (text.isBlank()) return@withContext text

                val appContext = context

                val options =
                    ProofreaderOptions
                        .builder(appContext)
                        .setInputType(ProofreaderOptions.InputType.KEYBOARD)
                        .setLanguage(ProofreaderOptions.Language.ENGLISH)
                        .build()
                val client: MlProofreaderClient = Proofreading.getClient(options)

                try {
                    progress.log(R.string.proofreader_log_on_device_check)
                    val status =
                        runCatching { client.checkFeatureStatus().await() }
                            .getOrDefault(FeatureStatus.UNAVAILABLE)

                    if (status == FeatureStatus.DOWNLOADABLE) {
                        Timber.i("MlKitProofreader: Feature downloadable, starting download")
                        progress.log(R.string.proofreader_log_on_device_download)
                        client.downloadFeature(
                            object : DownloadCallback {
                                override fun onDownloadStarted(bytesToDownload: Long) {
                                    Timber.i(
                                        "MlKitProofreader: Download started, %d bytes to download",
                                        bytesToDownload,
                                    )
                                }

                                override fun onDownloadFailed(e: GenAiException) {
                                    Timber.e("MlKitProofreader: Download failed with error: %s", e.message)
                                }

                                override fun onDownloadProgress(totalBytesDownloaded: Long) {
                                    Timber.i(
                                        "MlKitProofreader: Download progress: %d bytes downloaded",
                                        totalBytesDownloaded,
                                    )
                                }

                                override fun onDownloadCompleted() {
                                    Timber.i("MlKitProofreader: Download completed successfully")
                                }
                            },
                        )
                    }
                    if (status == FeatureStatus.UNAVAILABLE) {
                        Timber.v("MlKitProofreader: Feature unavailable, returning original text")
                        progress.log(R.string.proofreader_log_on_device_unavailable)
                        return@withContext text
                    }

                    progress.log(R.string.proofreader_log_on_device_running)
                    val request = ProofreadingRequest.builder(text).build()
                    val result = client.runInference(request).await()
                    val best = result.results.firstOrNull()?.text
                    progress.log(R.string.proofreader_log_done)
                    best ?: text
                } catch (_: Throwable) {
                    Timber.e("MlKitProofreader: Error during proofreading, returning original text")
                    progress.log(R.string.proofreader_log_failed)
                    text
                } finally {
                    client.close()
                }
            }
    }
