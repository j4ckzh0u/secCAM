package com.privacycamera.app.feature.album

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.privacycamera.app.domain.model.ProcessedPhoto
import com.privacycamera.app.domain.repository.PhotoRepository
import com.privacycamera.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PrivacyAlbumUiState(
    val photos: List<ProcessedPhoto> = emptyList(),
    val isUnlocked: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class PrivacyAlbumViewModel @Inject constructor(
    private val photoRepository: PhotoRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PrivacyAlbumUiState())
    val uiState: StateFlow<PrivacyAlbumUiState> = _uiState.asStateFlow()

    private var isPasswordSet = false

    init {
        checkPasswordStatus()
    }

    private fun checkPasswordStatus() {
        viewModelScope.launch {
            isPasswordSet = settingsRepository.isPasswordSet()
            if (!isPasswordSet) {
                _uiState.update { it.copy(isUnlocked = true) }
            }
        }
    }

    fun verifyPassword(password: String) {
        viewModelScope.launch {
            if (!isPasswordSet) {
                _uiState.update { it.copy(isUnlocked = true) }
                return@launch
            }

            val isValid = settingsRepository.verifyPassword(password)
            if (isValid) {
                _uiState.update { it.copy(isUnlocked = true, error = null) }
                loadPrivacyPhotos()
            } else {
                _uiState.update { it.copy(error = "密码错误") }
            }
        }
    }

    private fun loadPrivacyPhotos() {
        viewModelScope.launch {
            photoRepository.getPrivacyPhotos().collect { photos ->
                _uiState.update { it.copy(photos = photos) }
            }
        }
    }
}
