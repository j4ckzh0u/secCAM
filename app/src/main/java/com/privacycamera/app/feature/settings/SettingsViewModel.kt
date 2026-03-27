package com.privacycamera.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.privacycamera.app.domain.model.AppSettings
import com.privacycamera.app.domain.model.BlurLevel
import com.privacycamera.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val autoDetectEnabled: Boolean = true,
    val defaultBlurLevel: BlurLevel = BlurLevel.MEDIUM,
    val isPasswordSet: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            settingsRepository.getSettings().collect { settings ->
                _uiState.update {
                    it.copy(
                        autoDetectEnabled = settings.autoDetectEnabled,
                        defaultBlurLevel = settings.defaultProcessLevel,
                        isPasswordSet = settings.isPasswordSet
                    )
                }
            }
        }
    }

    fun setAutoDetect(enabled: Boolean) {
        viewModelScope.launch {
            val currentSettings = AppSettings(
                autoDetectEnabled = enabled,
                defaultProcessLevel = _uiState.value.defaultBlurLevel,
                isPasswordSet = _uiState.value.isPasswordSet
            )
            settingsRepository.updateSettings(currentSettings)
            _uiState.update { it.copy(autoDetectEnabled = enabled) }
        }
    }

    fun setBlurLevel(level: BlurLevel) {
        viewModelScope.launch {
            val currentSettings = AppSettings(
                autoDetectEnabled = _uiState.value.autoDetectEnabled,
                defaultProcessLevel = level,
                isPasswordSet = _uiState.value.isPasswordSet
            )
            settingsRepository.updateSettings(currentSettings)
            _uiState.update { it.copy(defaultBlurLevel = level) }
        }
    }

    fun setPassword(password: String) {
        viewModelScope.launch {
            settingsRepository.setPassword(password)
            _uiState.update { it.copy(isPasswordSet = true) }
        }
    }
}
