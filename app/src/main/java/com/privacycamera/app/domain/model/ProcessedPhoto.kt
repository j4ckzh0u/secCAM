package com.privacycamera.app.domain.model

data class ProcessedPhoto(
    val id: String,
    val originalUri: String,
    val processedUri: String,
    val sensitiveInfoList: List<SensitiveInfo>,
    val processedAt: Long,
    val isFromPrivacyAlbum: Boolean
)
