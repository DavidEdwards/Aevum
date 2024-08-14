package dae.aevum.domain.repositories

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import java.util.Locale
import javax.inject.Singleton

interface AudioRepository {
    suspend fun getAudioText(): String
}

@Singleton
class AudioRepositoryImpl(
    private val context: Context,
    private val jiraRepository: JiraRepository
) : AudioRepository {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override suspend fun getAudioText(): String {
        return suspendCancellableCoroutine { continuation ->
            var result = ""
            val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer.setRecognitionListener(object : RecognitionListener {
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
                    scope.launch {
                        delay(1_000L)
                        speechRecognizer.stopListening()
                        continuation.resumeWith(Result.success(result))
                    }
                }

                override fun onError(error: Int) {
                    Timber.v("Speech: onError: $error")
                    // TODO: Necessary?
                    // continuation.resumeWith(Result.failure(RuntimeException("Speech recognition reported an error of $error")))
                }

                override fun onResults(results: Bundle?) {
                    Timber.v("Speech: onResults: $results")

                    val data = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?: return
                    Timber.v("Speech: onResults: data: $data")
                    result = data.joinToString(" ")

                    scope.launch {
                        jiraRepository.stopLogging(result)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    Timber.v("Speech: onPartialResults: $partialResults")
                }

                override fun onEvent(eventType: Int, params: Bundle?) {
                    Timber.v("Speech: onEvent: $eventType, $params")
                }
            })

            val speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            speechRecognizerIntent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())

            speechRecognizer.startListening(speechRecognizerIntent)
        }
    }
}