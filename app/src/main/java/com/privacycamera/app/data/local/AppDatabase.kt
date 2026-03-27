package com.privacycamera.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ProcessedPhotoEntity::class, AppSettingsEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun processedPhotoDao(): ProcessedPhotoDao
    abstract fun appSettingsDao(): AppSettingsDao
}
