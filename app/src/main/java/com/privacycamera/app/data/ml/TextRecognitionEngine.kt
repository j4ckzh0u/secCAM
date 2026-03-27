package com.privacycamera.app.data.ml

import android.graphics.Bitmap
import android.graphics.RectF
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.privacycamera.app.domain.model.BoundingBox
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class TextRecognitionEngine @Inject constructor() {

    private val recognizer: TextRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    data class TextRecognitionResult(
        val text: String,
        val boundingBox: BoundingBox,
        val confidence: Float
    )

    suspend fun recognizeText(bitmap: Bitmap): List<TextRecognitionResult> {
        return suspendCancellableCoroutine { continuation ->
            val image = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val results = mutableListOf<TextRecognitionResult>()
                    for (block in visionText.textBlocks) {
                        for (line in block.lines) {
                            val rect = line.boundingBox
                            if (rect != null) {
                                results.add(
                                    TextRecognitionResult(
                                        text = line.text,
                                        boundingBox = BoundingBox(
                                            left = rect.left.toFloat(),
                                            top = rect.top.toFloat(),
                                            right = rect.right.toFloat(),
                                            bottom = rect.bottom.toFloat()
                                        ),
                                        confidence = 0.9f
                                    )
                                )
                            }
                        }
                    }
                    continuation.resume(results)
                }
                .addOnFailureListener { e ->
                    continuation.resumeWithException(e)
                }
        }
    }
}
