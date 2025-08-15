Artifact:

implementation("com.google.mlkit:genai-proofreading:1.0.0-beta1")

Example: 

```kotlin
val textToProofread = "The praject is compleet but needs too be reviewd"

// Define task with required input and output format
val options = ProofreaderOptions.builder(context)
    // InputType can be KEYBOARD or VOICE. VOICE indicates that
    // the user generated text based on audio input.
    .setInputType(ProofreaderOptions.InputType.KEYBOARD)
    // Refer to ProofreaderOptions.Language for available
    // languages
    .setLanguage(ProofreaderOptions.Language.ENGLISH)
    .build()
val proofreader = Proofreading.getClient(options)

suspend fun prepareAndStartProofread() {
    // Check feature availability, status will be one of the
    // following: UNAVAILABLE, DOWNLOADABLE, DOWNLOADING, AVAILABLE
    val featureStatus = proofreader.checkFeatureStatus().await()

    if (featureStatus == FeatureStatus.DOWNLOADABLE) {
        // Download feature if necessary.
        // If downloadFeature is not called, the first inference
        // request will also trigger the feature to be downloaded
        // if it's not already downloaded.
        proofreader.downloadFeature(object : DownloadCallback {
            override fun onDownloadStarted(bytesToDownload: Long) { }

            override fun onDownloadFailed(e: GenAiException) { }

            override fun onDownloadProgress(
                totalBytesDownloaded: Long
            ) {}

            override fun onDownloadCompleted() {
                startProofreadingRequest(textToProofread, proofreader)
            }
        })
    } else if (featureStatus == FeatureStatus.DOWNLOADING) {
        // Inference request will automatically run once feature is
        // downloaded.
        // If Gemini Nano is already downloaded on the device, the
        // feature-specific LoRA adapter model will be downloaded
        // very quickly. However, if Gemini Nano is not already
        // downloaded, the download process may take longer.
        startProofreadingRequest(textToProofread, proofreader)
    } else if (featureStatus == FeatureStatus.AVAILABLE) {
        startProofreadingRequest(textToProofread, proofreader)
    }
}

suspend fun startProofreadingRequest(
    text: String, proofreader: Proofreader
) {
    // Create task request
    val proofreadingRequest =
        ProofreadingRequest.builder(text).build()

    // Start proofreading request with non-streaming response
    // More than 1 result may be returned. If multiple suggestions
    // are returned, results will be sorted by descending confidence.
    val proofreadingResults =
        proofreader.runInference(proofreadingRequest).await().results

    // You can also start a streaming request
    // proofreader.runInference(proofreadingRequest) { newText ->
    //     // show new text in UI
    // }
}

// Be sure to release the resource when no longer needed
// For example, on viewModel.onCleared() or activity.onDestroy()
proofreader.close()
```