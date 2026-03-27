package com.privacycamera.app.feature.album

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.privacycamera.app.domain.model.ProcessedPhoto
import com.privacycamera.app.domain.repository.PhotoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class AlbumUiState(
    val photos: List<ProcessedPhoto> = emptyList(),
    val selectedPhotoUri: Uri? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AlbumViewModel @Inject constructor(
    private val photoRepository: PhotoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlbumUiState())
    val uiState: StateFlow<AlbumUiState> = _uiState.asStateFlow()

    init {
        loadPhotos()
    }

    private fun loadPhotos() {
        viewModelScope.launch {
            photoRepository.getAllPhotos().collect { photos ->
                _uiState.update { it.copy(photos = photos) }
            }
        }
    }

    fun onPhotoSelected(uri: Uri) {
        _uiState.update { it.copy(selectedPhotoUri = uri) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
