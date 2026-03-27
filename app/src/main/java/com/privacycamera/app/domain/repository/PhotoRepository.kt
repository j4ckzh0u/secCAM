package com.privacycamera.app.domain.repository

import com.privacycamera.app.domain.model.ProcessedPhoto
import kotlinx.coroutines.flow.Flow

interface PhotoRepository {
    fun getAllPhotos(): Flow<List<ProcessedPhoto>>
    fun getPrivacyPhotos(): Flow<List<ProcessedPhoto>>
    suspend fun getPhotoById(id: String): ProcessedPhoto?
    suspend fun savePhoto(photo: ProcessedPhoto)
    suspend fun deletePhoto(id: String)
}
