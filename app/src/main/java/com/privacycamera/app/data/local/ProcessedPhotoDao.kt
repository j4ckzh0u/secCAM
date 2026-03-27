package com.privacycamera.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProcessedPhotoDao {
    @Query("SELECT * FROM processed_photos ORDER BY processedAt DESC")
    fun getAllPhotos(): Flow<List<ProcessedPhotoEntity>>

    @Query("SELECT * FROM processed_photos WHERE isFromPrivacyAlbum = 1 ORDER BY processedAt DESC")
    fun getPrivacyPhotos(): Flow<List<ProcessedPhotoEntity>>

    @Query("SELECT * FROM processed_photos WHERE id = :id")
    suspend fun getPhotoById(id: String): ProcessedPhotoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: ProcessedPhotoEntity)

    @Query("DELETE FROM processed_photos WHERE id = :id")
    suspend fun deletePhoto(id: String)
}
