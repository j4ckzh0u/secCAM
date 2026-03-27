package com.privacycamera.app.feature.processing

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.BlurMaskFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.privacycamera.app.data.ml.SensitiveInfoDetector
import com.privacycamera.app.domain.model.BlurLevel
import com.privacycamera.app.domain.model.ProcessedPhoto
import com.privacycamera.app.domain.model.SensitiveInfo
import com.privacycamera.app.domain.model.SensitiveType
import com.privacycamera.app.domain.repository.PhotoRepository
import com.privacycamera.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.UUID
import javax.inject.Inject

data class ProcessingUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val originalBitmap: Bitmap? = null,
    val processedBitmap: Bitmap? = null,
    val detectedSensitiveInfo: List<SensitiveInfo> = emptyList(),
    val showProcessed: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null,
    val blurLevel: BlurLevel = BlurLevel.MEDIUM,
    val saveOriginal: Boolean = false
)

@HiltViewModel
class ProcessingViewModel @Inject constructor(
    private val sensitiveInfoDetector: SensitiveInfoDetector,
    private val photoRepository: PhotoRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProcessingUiState())
    val uiState: StateFlow<ProcessingUiState> = _uiState.asStateFlow()

    private var currentPhotoPath: String? = null

    fun loadPhoto(photoUriString: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val settings = settingsRepository.getSettings().first()
                val blurLevel = settings.defaultProcessLevel

                val uri = Uri.parse(photoUriString)
                val path = uri.path

                if (path.isNullOrEmpty()) {
                    _uiState.update { it.copy(error = "照片路径无效", isLoading = false) }
                    return@launch
                }

                currentPhotoPath = path
                val file = File(path)

                if (!file.exists()) {
                    _uiState.update { it.copy(error = "照片文件不存在", isLoading = false) }
                    return@launch
                }

                val bitmap = BitmapFactory.decodeFile(path)
                if (bitmap == null) {
                    _uiState.update { it.copy(error = "无法读取照片", isLoading = false) }
                    return@launch
                }

                val sensitiveList = mutableListOf<SensitiveInfo>()

                val gpsInfo = sensitiveInfoDetector.detectGpsFromFile(file)
                if (gpsInfo != null) {
                    sensitiveList.add(gpsInfo)
                }

                val textSensitive = sensitiveInfoDetector.detectSensitiveInfoFromFile(file)
                sensitiveList.addAll(textSensitive)

                val processedBitmap = applyBlur(bitmap, sensitiveList, blurLevel)

                _uiState.update {
                    it.copy(
                        originalBitmap = bitmap,
                        processedBitmap = processedBitmap,
                        detectedSensitiveInfo = sensitiveList,
                        isLoading = false,
                        showProcessed = true,
                        blurLevel = blurLevel
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = "处理失败: ${e.message}",
                        isLoading = false
                    )
                }
            }
        }
    }

    fun setBlurLevel(level: BlurLevel) {
        viewModelScope.launch {
            val currentBitmap = _uiState.value.originalBitmap ?: return@launch
            val sensitiveList = _uiState.value.detectedSensitiveInfo

            val processedBitmap = applyBlur(currentBitmap, sensitiveList, level)

            _uiState.update {
                it.copy(
                    processedBitmap = processedBitmap,
                    blurLevel = level
                )
            }
        }
    }

    fun toggleSaveOriginal(saveOriginal: Boolean) {
        _uiState.update { it.copy(saveOriginal = saveOriginal) }
    }

    private fun applyBlur(bitmap: Bitmap, sensitiveList: List<SensitiveInfo>, blurLevel: BlurLevel): Bitmap {
        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)

        val blurRadius = when (blurLevel) {
            BlurLevel.LIGHT -> 10f
            BlurLevel.MEDIUM -> 25f
            BlurLevel.HEAVY -> 50f
        }

        for (info in sensitiveList) {
            if (info.type == SensitiveType.GPS_LOCATION) {
                continue
            }

            val box = info.boundingBox

            val padding = (box.width * 0.15f).coerceAtLeast(20f)
            val left = ((box.left - padding) * bitmap.width).toInt().coerceAtLeast(0)
            val top = ((box.top - padding) * bitmap.height).toInt().coerceAtLeast(0)
            val right = ((box.right + padding) * bitmap.width).toInt().coerceAtMost(bitmap.width)
            val bottom = ((box.bottom + padding) * bitmap.height).toInt().coerceAtMost(bitmap.height)

            if (right <= left || bottom <= top) continue

            val width = right - left
            val height = bottom - top

            if (width <= 0 || height <= 0) continue

            val rect = android.graphics.RectF(
                left.toFloat(), top.toFloat(),
                right.toFloat(), bottom.toFloat()
            )

            val paint = Paint().apply {
                isAntiAlias = true
                maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
            }

            canvas.drawRect(rect, paint)
        }

        return mutableBitmap
    }

    fun clearGpsFromPhoto() {
        viewModelScope.launch {
            val path = currentPhotoPath ?: return@launch
            val file = File(path)

            val success = sensitiveInfoDetector.removeGpsFromFile(file)
            if (success) {
                val updatedList = _uiState.value.detectedSensitiveInfo.filter {
                    it.type != SensitiveType.GPS_LOCATION
                }
                _uiState.update {
                    it.copy(detectedSensitiveInfo = updatedList)
                }
            }
        }
    }

    fun saveProcessedPhoto(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }

            try {
                val bitmapToSave = if (_uiState.value.saveOriginal) {
                    _uiState.value.originalBitmap
                } else {
                    _uiState.value.processedBitmap
                }

                if (bitmapToSave == null) {
                    _uiState.update { it.copy(isSaving = false, error = "没有可保存的照片") }
                    return@launch
                }

                val savedUri = withContext(Dispatchers.IO) {
                    saveBitmapToGallery(context, bitmapToSave)
                }

                if (savedUri != null) {
                    val photo = ProcessedPhoto(
                        id = UUID.randomUUID().toString(),
                        originalUri = currentPhotoPath ?: "",
                        processedUri = savedUri,
                        sensitiveInfoList = _uiState.value.detectedSensitiveInfo,
                        processedAt = System.currentTimeMillis(),
                        isFromPrivacyAlbum = true
                    )
                    photoRepository.savePhoto(photo)

                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            saveSuccess = true
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = "保存失败"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = "保存失败: ${e.message}"
                    )
                }
            }
        }
    }

    private fun saveBitmapToGallery(context: Context, bitmap: Bitmap): String? {
        val filename = "PrivacyCamera_${System.currentTimeMillis()}.jpg"

        val outputStream: OutputStream?
        var uri: Uri? = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/PrivacyCamera")
            }

            uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            outputStream = uri?.let { context.contentResolver.openOutputStream(it) }
        } else {
            val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val privacyDir = File(imagesDir, "PrivacyCamera")
            if (!privacyDir.exists()) {
                privacyDir.mkdirs()
            }
            val imageFile = File(privacyDir, filename)
            outputStream = FileOutputStream(imageFile)
            uri = Uri.fromFile(imageFile)
        }

        return try {
            outputStream?.use { stream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
            }
            uri?.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
