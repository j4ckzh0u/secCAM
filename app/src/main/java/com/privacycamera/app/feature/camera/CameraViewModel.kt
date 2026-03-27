package com.privacycamera.app.feature.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.privacycamera.app.data.ml.SensitiveInfoDetector
import com.privacycamera.app.domain.model.SensitiveInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import javax.inject.Inject

data class CameraUiState(
    val isLoading: Boolean = false,
    val flashMode: FlashMode = FlashMode.AUTO,
    val isFrontCamera: Boolean = false,
    val detectedSensitiveInfo: List<SensitiveInfo> = emptyList(),
    val capturedPhotoUri: Uri? = null,
    val capturedPhotoBitmap: Bitmap? = null,
    val hasGpsInfo: Boolean = false,
    val error: String? = null,
    val showSensitiveDialog: Boolean = false,
    val isProcessing: Boolean = false
)

@HiltViewModel
class CameraViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sensitiveInfoDetector: SensitiveInfoDetector
) : ViewModel() {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    val cameraManager = CameraManager(context)

    fun toggleFlashMode() {
        viewModelScope.launch {
            val newMode = when (_uiState.value.flashMode) {
                FlashMode.AUTO -> FlashMode.ON
                FlashMode.ON -> FlashMode.OFF
                FlashMode.OFF -> FlashMode.AUTO
            }
            cameraManager.setFlashMode(newMode)
            _uiState.update { it.copy(flashMode = newMode) }
        }
    }

    fun switchCamera() {
        viewModelScope.launch {
            _uiState.update { it.copy(isFrontCamera = !it.isFrontCamera) }
        }
    }

    fun onPhotoCaptured(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            try {
                val file = File(uri.path ?: return@launch)
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    ?: return@launch

                val sensitiveList = mutableListOf<SensitiveInfo>()

                val gpsInfo = sensitiveInfoDetector.detectGpsFromFile(file)
                if (gpsInfo != null) {
                    sensitiveList.add(gpsInfo)
                    _uiState.update { it.copy(hasGpsInfo = true) }
                }

                val textSensitive = sensitiveInfoDetector.detectSensitiveInfo(bitmap)
                sensitiveList.addAll(textSensitive)

                _uiState.update {
                    it.copy(
                        capturedPhotoUri = uri,
                        capturedPhotoBitmap = bitmap,
                        detectedSensitiveInfo = sensitiveList,
                        isProcessing = false,
                        showSensitiveDialog = sensitiveList.isNotEmpty()
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = "处理照片失败: ${e.message}",
                        isProcessing = false
                    )
                }
            }
        }
    }

    fun dismissSensitiveDialog() {
        _uiState.update { it.copy(showSensitiveDialog = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun createTempPhotoFile(): File {
        val photoDir = File(context.cacheDir, "photos")
        if (!photoDir.exists()) {
            photoDir.mkdirs()
        }
        return File(photoDir, "photo_${UUID.randomUUID()}.jpg")
    }

    override fun onCleared() {
        super.onCleared()
        cameraManager.release()
    }
}
