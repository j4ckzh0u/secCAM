package com.privacycamera.app.data.repository

import com.privacycamera.app.data.local.ProcessedPhotoDao
import com.privacycamera.app.data.local.ProcessedPhotoEntity
import com.privacycamera.app.domain.model.ProcessedPhoto
import com.privacycamera.app.domain.model.SensitiveInfo
import com.privacycamera.app.domain.model.SensitiveType
import com.privacycamera.app.domain.repository.PhotoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhotoRepositoryImpl @Inject constructor(
    private val photoDao: ProcessedPhotoDao
) : PhotoRepository {

    override fun getAllPhotos(): Flow<List<ProcessedPhoto>> {
        return photoDao.getAllPhotos().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getPrivacyPhotos(): Flow<List<ProcessedPhoto>> {
        return photoDao.getPrivacyPhotos().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getPhotoById(id: String): ProcessedPhoto? {
        return photoDao.getPhotoById(id)?.toDomainModel()
    }

    override suspend fun savePhoto(photo: ProcessedPhoto) {
        photoDao.insertPhoto(photo.toEntity())
    }

    override suspend fun deletePhoto(id: String) {
        photoDao.deletePhoto(id)
    }

    private fun ProcessedPhotoEntity.toDomainModel(): ProcessedPhoto {
        val sensitiveTypes = this.sensitiveTypes.split(",")
            .filter { it.isNotBlank() }
            .mapNotNull { typeName ->
                try {
                    SensitiveType.valueOf(typeName.trim())
                } catch (e: Exception) {
                    null
                }
            }
        return ProcessedPhoto(
            id = id,
            originalUri = originalUri,
            processedUri = processedUri,
            sensitiveInfoList = sensitiveTypes.map { type ->
                SensitiveInfo(
                    type = type,
                    boundingBox = com.privacycamera.app.domain.model.BoundingBox(0f, 0f, 0f, 0f),
                    content = "",
                    confidence = 1.0f
                )
            },
            processedAt = processedAt,
            isFromPrivacyAlbum = isFromPrivacyAlbum
        )
    }

    private fun ProcessedPhoto.toEntity(): ProcessedPhotoEntity {
        return ProcessedPhotoEntity(
            id = id,
            originalUri = originalUri,
            processedUri = processedUri,
            sensitiveTypes = sensitiveInfoList.joinToString(",") { it.type.name },
            processedAt = processedAt,
            isFromPrivacyAlbum = isFromPrivacyAlbum
        )
    }
}
