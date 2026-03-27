package com.privacycamera.app.domain.model

data class SensitiveInfo(
    val type: SensitiveType,
    val boundingBox: BoundingBox,
    val content: String,
    val confidence: Float
)

data class BoundingBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
}
