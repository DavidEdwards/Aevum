package dae.aevum.ml

import android.content.Context
import com.google.mlkit.genai.common.DownloadCallback
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.common.GenAiException
import com.google.mlkit.genai.proofreading.ProofreaderOptions
import com.google.mlkit.genai.proofreading.Proofreading
import com.google.mlkit.genai.proofreading.ProofreadingRequest
import dae.aevum.domain.repositories.Proofreader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import com.google.mlkit.genai.proofreading.Proofreader as MlProofreaderClient

class MlKitProofreader(
    private val context: Context
) : Proofreader {
    override suspend fun proofread(text: String): String = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext text

        val appContext = context

        val options = ProofreaderOptions.builder(appContext)
            .setInputType(ProofreaderOptions.InputType.KEYBOARD)
            .setLanguage(ProofreaderOptions.Language.ENGLISH)
            .build()
        val client: MlProofreaderClient = Proofreading.getClient(options)

        try {
            val status = runCatching { client.checkFeatureStatus().await() }
                .getOrDefault(FeatureStatus.UNAVAILABLE)

            if (status == FeatureStatus.DOWNLOADABLE) {
                Timber.i("MlKitProofreader: Feature downloadable, starting download")
                // Trigger download in background; inference can proceed and will wait as needed.
                client.downloadFeature(object : DownloadCallback {
                    override fun onDownloadStarted(bytesToDownload: Long) {
                        Timber.i(
                            "MlKitProofreader: Download started, %d bytes to download",
                            bytesToDownload
                        )
                    }

                    override fun onDownloadFailed(e: GenAiException) {
                        Timber.e("MlKitProofreader: Download failed with error: %s", e.message)
                    }

                    override fun onDownloadProgress(totalBytesDownloaded: Long) {
                        Timber.i(
                            "MlKitProofreader: Download progress: %d bytes downloaded",
                            totalBytesDownloaded
                        )
                    }

                    override fun onDownloadCompleted() {
                        Timber.i("MlKitProofreader: Download completed successfully")
                    }
                })
            }
            if (status == FeatureStatus.UNAVAILABLE) {
                Timber.v("MlKitProofreader: Feature unavailable, returning original text")
                return@withContext text
            }

            val request = ProofreadingRequest.builder(text).build()
            val result = client.runInference(request).await()
            val best = result.results.firstOrNull()?.text
            best ?: text
        } catch (_: Throwable) {
            Timber.e("MlKitProofreader: Error during proofreading, returning original text")
            text
        } finally {
            client.close()
        }
    }
}
