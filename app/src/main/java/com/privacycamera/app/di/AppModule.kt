package com.privacycamera.app.di

import android.content.Context
import androidx.room.Room
import com.privacycamera.app.data.local.AppDatabase
import com.privacycamera.app.data.local.AppSettingsDao
import com.privacycamera.app.data.local.ProcessedPhotoDao
import com.privacycamera.app.data.repository.PhotoRepositoryImpl
import com.privacycamera.app.data.repository.SettingsRepositoryImpl
import com.privacycamera.app.domain.repository.PhotoRepository
import com.privacycamera.app.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "privacy_camera_db"
        ).build()
    }

    @Provides
    fun provideProcessedPhotoDao(database: AppDatabase): ProcessedPhotoDao {
        return database.processedPhotoDao()
    }

    @Provides
    fun provideAppSettingsDao(database: AppDatabase): AppSettingsDao {
        return database.appSettingsDao()
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPhotoRepository(impl: PhotoRepositoryImpl): PhotoRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}
