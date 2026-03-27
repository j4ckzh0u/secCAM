package com.privacycamera.app.feature.processing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.BlurMaskFilter
import com.privacycamera.app.domain.model.BlurLevel
import com.privacycamera.app.domain.model.BoundingBox
import com.privacycamera.app.domain.model.SensitiveInfo
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class BlurProcessor @Inject constructor() {

    fun applyBlur(
        bitmap: Bitmap,
        sensitiveInfoList: List<SensitiveInfo>,
        blurLevel: BlurLevel
    ): Bitmap {
        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)

        val blurRadius = when (blurLevel) {
            BlurLevel.LIGHT -> 10f
            BlurLevel.MEDIUM -> 25f
            BlurLevel.HEAVY -> 50f
        }

        for (info in sensitiveInfoList) {
            if (info.type == com.privacycamera.app.domain.model.SensitiveType.GPS_LOCATION) {
                continue
            }

            val box = scaleBoundingBox(info.boundingBox, bitmap.width.toFloat(), bitmap.height.toFloat())

            val padding = (box.width * 0.1f).coerceAtLeast(10f)
            val left = (box.left - padding).coerceAtLeast(0f)
            val top = (box.top - padding).coerceAtLeast(0f)
            val right = (box.right + padding).coerceAtMost(bitmap.width.toFloat())
            val bottom = (box.bottom + padding).coerceAtMost(bitmap.height.toFloat())

            val rect = RectF(left, top, right, bottom)

            val paint = Paint().apply {
                isAntiAlias = true
                maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
            }

            canvas.drawRoundRect(rect, 8f, 8f, paint)
        }

        return mutableBitmap
    }

    fun applyBlurToRegion(
        bitmap: Bitmap,
        region: BoundingBox,
        blurLevel: BlurLevel
    ): Bitmap {
        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)

        val blurRadius = when (blurLevel) {
            BlurLevel.LIGHT -> 10f
            BlurLevel.MEDIUM -> 25f
            BlurLevel.HEAVY -> 50f
        }

        val scaledBox = scaleBoundingBox(region, bitmap.width.toFloat(), bitmap.height.toFloat())
        val rect = RectF(scaledBox.left, scaledBox.top, scaledBox.right, scaledBox.bottom)

        val paint = Paint().apply {
            isAntiAlias = true
            maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
        }

        canvas.drawRoundRect(rect, 8f, 8f, paint)

        return mutableBitmap
    }

    private fun scaleBoundingBox(
        box: BoundingBox,
        targetWidth: Float,
        targetHeight: Float
    ): BoundingBox {
        return BoundingBox(
            left = box.left * targetWidth,
            top = box.top * targetHeight,
            right = box.right * targetWidth,
            bottom = box.bottom * targetHeight
        )
    }
}
