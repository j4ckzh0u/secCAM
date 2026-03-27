package com.privacycamera.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "processed_photos")
data class ProcessedPhotoEntity(
    @PrimaryKey val id: String,
    val originalUri: String,
    val processedUri: String,
    val sensitiveTypes: String,
    val processedAt: Long,
    val isFromPrivacyAlbum: Boolean
)
