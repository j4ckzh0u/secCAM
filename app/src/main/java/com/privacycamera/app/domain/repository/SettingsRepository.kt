package com.privacycamera.app.domain.repository

import com.privacycamera.app.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getSettings(): Flow<AppSettings>
    suspend fun updateSettings(settings: AppSettings)
    suspend fun setPassword(password: String)
    suspend fun verifyPassword(password: String): Boolean
    suspend fun isPasswordSet(): Boolean
}
