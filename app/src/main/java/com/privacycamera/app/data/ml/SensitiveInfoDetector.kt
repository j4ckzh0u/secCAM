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
            "\\b[1-9]\\d{5}(?:19|20)\\d{2}(?:0[1-9]|1[0-2])(?:0[1-9]|[12]\\d|3[01])\\d{3}[\\dXx]\\b"
        )

        private val BANK_CARD_REGEX = Regex("\\b\\d{16,19}\\b")

        private val PHONE_REGEX = Regex("\\b1[3-9]\\d{9}\\b")

        private val EMAIL_REGEX = Regex("\\b[\\w.-]+@[\\w.-]+\\.\\w+\\b")

        private val ADDRESS_KEYWORDS = listOf("省", "市", "区", "县", "路", "街", "号", "栋", "楼", "室")

        private val PASSWORD_PATTERN = Regex("(?i)(password|pwd|密码|pass|口令)[:=]?\\s*(\\S+)")
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

    private fun classifyText(text: String): SensitiveType? {
        return when {
            ID_CARD_REGEX.containsMatchIn(text) -> SensitiveType.ID_CARD
            BANK_CARD_REGEX.containsMatchIn(text) && isValidBankCard(text) -> SensitiveType.BANK_CARD
            PHONE_REGEX.containsMatchIn(text) -> SensitiveType.PHONE_NUMBER
            EMAIL_REGEX.containsMatchIn(text) -> SensitiveType.EMAIL
            PASSWORD_PATTERN.containsMatchIn(text) -> SensitiveType.PASSWORD
            isPotentialAddress(text) -> SensitiveType.ADDRESS
            else -> null
        }
    }

    private fun isValidBankCard(cardNumber: String): Boolean {
        val digitsOnly = cardNumber.replace("\\s".toRegex(), "")
        if (!digitsOnly.matches("\\d{16,19}".toRegex())) return false
        return luhnCheck(digitsOnly)
    }

    private fun luhnCheck(cardNumber: String): Boolean {
        var sum = 0
        var alternate = false
        for (i in cardNumber.length - 1 downTo 0) {
            var digit = cardNumber[i].digitToInt()
            if (alternate) {
                digit *= 2
                if (digit > 9) {
                    digit = (digit % 10) + 1
                }
            }
            sum += digit
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
