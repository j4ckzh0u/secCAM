package com.privacycamera.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val autoDetectEnabled: Boolean = true,
    val defaultProcessLevel: String = "MEDIUM",
    val passwordHash: String? = null,
    val passwordSalt: String? = null
)
