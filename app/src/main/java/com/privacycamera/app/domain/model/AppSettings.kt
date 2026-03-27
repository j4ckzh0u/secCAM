package com.privacycamera.app.domain.model

enum class BlurLevel {
    LIGHT,
    MEDIUM,
    HEAVY
}

data class AppSettings(
    val autoDetectEnabled: Boolean = true,
    val defaultProcessLevel: BlurLevel = BlurLevel.MEDIUM,
    val isPasswordSet: Boolean = false
)
