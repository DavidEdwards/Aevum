package dae.aevum.domain.repositories

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Singleton
import kotlin.coroutines.resume

interface AudioRepository {
    suspend fun getAudioText(): String
}

@Singleton
class AudioRepositoryImpl(
    private val context: Context,
    @Suppress("UNUSED_PARAMETER") private val jiraRepository: JiraRepository,
) : AudioRepository {
    override suspend fun getAudioText(): String {
        return suspendCancellableCoroutine { continuation ->
            val completed = AtomicBoolean(false)
            fun complete(value: String) {
                if (completed.compareAndSet(false, true)) {
                    continuation.resume(value)
                }
            }

            val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            continuation.invokeOnCancellation {
                speechRecognizer.cancel()
                speechRecognizer.destroy()
            }

            speechRecognizer.setRecognitionListener(
                object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        Timber.v("Speech: onReadyForSpeech: $params")
                    }

                    override fun onBeginningOfSpeech() {
                        Timber.v("Speech: onBeginningOfSpeech")
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        Timber.v("Speech: onRmsChanged: $rmsdB")
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {
                        Timber.v("Speech: onBufferReceived: ${buffer?.size}")
                    }

                    override fun onEndOfSpeech() {
                        Timber.v("Speech: onEndOfSpeech")
                    }

                    override fun onError(error: Int) {
                        Timber.v("Speech: onError: $error")
                        speechRecognizer.destroy()
                        complete("")
                    }

                    override fun onResults(results: Bundle?) {
                        Timber.v("Speech: onResults: $results")
                        val data =
                            results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                .orEmpty()
                        val text = data.joinToString(" ")
                        Timber.v("Speech: onResults: data: $data")
                        speechRecognizer.destroy()
                        // Caller (CompletedWorklogDialog) stops the timer with this text.
                        complete(text)
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        Timber.v("Speech: onPartialResults: $partialResults")
                    }

                    override fun onEvent(
                        eventType: Int,
                        params: Bundle?,
                    ) {
                        Timber.v("Speech: onEvent: $eventType, $params")
                    }
                },
            )

            val speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            speechRecognizerIntent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
            )
            speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())

            speechRecognizer.startListening(speechRecognizerIntent)
        }
    }
}
