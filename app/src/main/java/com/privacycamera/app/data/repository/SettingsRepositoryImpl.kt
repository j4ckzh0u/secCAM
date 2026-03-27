package com.privacycamera.app.data.repository

import com.privacycamera.app.data.local.AppSettingsDao
import com.privacycamera.app.data.local.AppSettingsEntity
import com.privacycamera.app.data.security.PasswordManager
import com.privacycamera.app.domain.model.AppSettings
import com.privacycamera.app.domain.model.BlurLevel
import com.privacycamera.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val settingsDao: AppSettingsDao,
    private val passwordManager: PasswordManager
) : SettingsRepository {

    override fun getSettings(): Flow<AppSettings> {
        return settingsDao.getSettings().map { entity ->
            entity?.toDomainModel() ?: AppSettings()
        }
    }

    override suspend fun updateSettings(settings: AppSettings) {
        settingsDao.insertSettings(settings.toEntity())
    }

    override suspend fun setPassword(password: String) {
        passwordManager.setPassword(password)
        val hash = passwordManager.getStoredHash()
        val salt = passwordManager.getStoredSalt()
        if (hash != null && salt != null) {
            val entity = AppSettingsEntity(
                autoDetectEnabled = true,
                defaultProcessLevel = "MEDIUM",
                passwordHash = hash,
                passwordSalt = salt
            )
            settingsDao.insertSettings(entity)
        }
    }

    override suspend fun verifyPassword(password: String): Boolean {
        return passwordManager.verifyPassword(password)
    }

    override suspend fun isPasswordSet(): Boolean {
        return passwordManager.isPasswordSet()
    }

    private fun AppSettingsEntity.toDomainModel(): AppSettings {
        if (passwordHash != null && passwordSalt != null) {
            passwordManager.setStoredCredentials(passwordHash, passwordSalt)
        }
        return AppSettings(
            autoDetectEnabled = autoDetectEnabled,
            defaultProcessLevel = try {
                BlurLevel.valueOf(defaultProcessLevel)
            } catch (e: Exception) {
                BlurLevel.MEDIUM
            },
            isPasswordSet = passwordHash != null
        )
    }

    private fun AppSettings.toEntity(): AppSettingsEntity {
        return AppSettingsEntity(
            autoDetectEnabled = autoDetectEnabled,
            defaultProcessLevel = defaultProcessLevel.name,
            passwordHash = passwordManager.getStoredHash(),
            passwordSalt = passwordManager.getStoredSalt()
        )
    }
}
