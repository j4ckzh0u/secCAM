package com.privacycamera.app.data.ml

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.privacycamera.app.domain.model.BoundingBox
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
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
                        val blockText = block.text
                        val blockRect = block.boundingBox
                        if (blockRect != null && blockText.isNotBlank()) {
                            results.add(
                                TextRecognitionResult(
                                    text = blockText,
                                    boundingBox = BoundingBox(
                                        left = blockRect.left.toFloat(),
                                        top = blockRect.top.toFloat(),
                                        right = blockRect.right.toFloat(),
                                        bottom = blockRect.bottom.toFloat()
                                    ),
                                    confidence = 0.9f
                                )
                            )
                        }

                        for (line in block.lines) {
                            val lineText = line.text
                            val lineRect = line.boundingBox
                            if (lineRect != null && lineText.isNotBlank() && lineText != blockText) {
                                results.add(
                                    TextRecognitionResult(
                                        text = lineText,
                                        boundingBox = BoundingBox(
                                            left = lineRect.left.toFloat(),
                                            top = lineRect.top.toFloat(),
                                            right = lineRect.right.toFloat(),
                                            bottom = lineRect.bottom.toFloat()
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

    suspend fun recognizeTextFromFile(file: File): List<TextRecognitionResult> {
        return suspendCancellableCoroutine { continuation ->
            try {
                val exif = ExifInterface(file)
                val orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )

                val bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                if (bitmap == null) {
                    continuation.resumeWithException(Exception("Cannot decode bitmap"))
                    return@suspendCancellableCoroutine
                }

                val rotatedBitmap = when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(bitmap, 90f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180f)
                    ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270f)
                    else -> bitmap
                }

                val image = InputImage.fromBitmap(rotatedBitmap, 0)
                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        val results = mutableListOf<TextRecognitionResult>()

                        for (block in visionText.textBlocks) {
                            val blockText = block.text
                            val blockRect = block.boundingBox
                            if (blockRect != null && blockText.isNotBlank()) {
                                results.add(
                                    TextRecognitionResult(
                                        text = blockText,
                                        boundingBox = BoundingBox(
                                            left = blockRect.left.toFloat(),
                                            top = blockRect.top.toFloat(),
                                            right = blockRect.right.toFloat(),
                                            bottom = blockRect.bottom.toFloat()
                                        ),
                                        confidence = 0.9f
                                    )
                                )
                            }
                        }
                        continuation.resume(results)
                    }
                    .addOnFailureListener { e ->
                        continuation.resumeWithException(e)
                    }
            } catch (e: Exception) {
                continuation.resumeWithException(e)
            }
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
