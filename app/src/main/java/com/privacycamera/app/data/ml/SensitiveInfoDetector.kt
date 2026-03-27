package com.privacycamera.app.data.ml

import android.graphics.Bitmap
import com.privacycamera.app.domain.model.BoundingBox
import com.privacycamera.app.domain.model.SensitiveInfo
import com.privacycamera.app.domain.model.SensitiveType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SensitiveInfoDetector @Inject constructor(
    private val exifMetadataReader: ExifMetadataReader,
    private val textRecognitionEngine: TextRecognitionEngine
) {

    companion object {
        private val ID_CARD_REGEX = Regex(
            "[1-9]\\d{5}(?:19|20)\\d{2}(?:0[1-9]|1[0-2])(?:0[1-9]|[12]\\d|3[01])\\d{3}[\\dXx]"
        )

        private val PHONE_REGEX = Regex("1[3-9]\\d{9}")

        private val EMAIL_REGEX = Regex("[\\w.-]+@[\\w.-]+\\.\\w+")

        private val ADDRESS_KEYWORDS = listOf("省", "市", "区", "县", "路", "街", "号", "栋", "楼", "室", "街道", "社区")

        private val PASSWORD_KEYWORDS = listOf("password", "pwd", "密码", "pass", "口令", "secret")
    }

    suspend fun detectSensitiveInfo(bitmap: Bitmap): List<SensitiveInfo> {
        val result = mutableListOf<SensitiveInfo>()

        val textResults = textRecognitionEngine.recognizeText(bitmap)
        for (textResult in textResults) {
            val sensitiveType = classifyText(textResult.text)
            if (sensitiveType != null) {
                result.add(
                    SensitiveInfo(
                        type = sensitiveType,
                        boundingBox = textResult.boundingBox,
                        content = textResult.text,
                        confidence = textResult.confidence
                    )
                )
            }
        }

        return result
    }

    suspend fun detectSensitiveInfoFromFile(file: java.io.File): List<SensitiveInfo> {
        val result = mutableListOf<SensitiveInfo>()

        try {
            val textResults = textRecognitionEngine.recognizeTextFromFile(file)
            for (textResult in textResults) {
                val sensitiveType = classifyText(textResult.text)
                if (sensitiveType != null) {
                    result.add(
                        SensitiveInfo(
                            type = sensitiveType,
                            boundingBox = textResult.boundingBox,
                            content = textResult.text,
                            confidence = textResult.confidence
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return result
    }

    private fun classifyText(text: String): SensitiveType? {
        val cleanText = text.replace("\\s+".toRegex(), " ").trim()

        if (ID_CARD_REGEX.containsMatchIn(cleanText)) {
            return SensitiveType.ID_CARD
        }

        val digitsOnly = cleanText.replace("[^0-9]".toRegex(), "")
        if (digitsOnly.length in 16..19 && luhnCheck(digitsOnly)) {
            return SensitiveType.BANK_CARD
        }

        if (PHONE_REGEX.containsMatchIn(cleanText)) {
            return SensitiveType.PHONE_NUMBER
        }

        if (EMAIL_REGEX.containsMatchIn(cleanText)) {
            return SensitiveType.EMAIL
        }

        for (keyword in PASSWORD_KEYWORDS) {
            if (cleanText.contains(keyword, ignoreCase = true)) {
                return SensitiveType.PASSWORD
            }
        }

        if (isPotentialAddress(cleanText)) {
            return SensitiveType.ADDRESS
        }

        return null
    }

    private fun luhnCheck(cardNumber: String): Boolean {
        if (cardNumber.length < 13 || cardNumber.length > 19) return false
        var sum = 0
        var alternate = false
        for (i in cardNumber.length - 1 downTo 0) {
            val digit = cardNumber[i].digitToIntOrNull() ?: return false
            if (alternate) {
                val doubled = digit * 2
                sum += if (doubled > 9) doubled - 9 else doubled
            } else {
                sum += digit
            }
            alternate = !alternate
        }
        return sum % 10 == 0
    }

    private fun isPotentialAddress(text: String): Boolean {
        var matchCount = 0
        for (keyword in ADDRESS_KEYWORDS) {
            if (text.contains(keyword)) {
                matchCount++
            }
        }
        return matchCount >= 2
    }

    suspend fun detectGpsFromFile(file: java.io.File): SensitiveInfo? {
        return exifMetadataReader.readGpsLocation(file)
    }

    fun removeGpsFromFile(file: java.io.File): Boolean {
        return exifMetadataReader.removeGpsData(file)
    }
}
