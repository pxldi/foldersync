package com.foldersync.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import com.foldersync.data.db.FolderSyncDatabase
import com.foldersync.data.db.dao.FileMetadataDao
import com.foldersync.data.db.dao.SyncProfileDao
import com.foldersync.data.db.dao.SyncRunDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FolderSyncDatabase {
        return Room.databaseBuilder(
            context,
            FolderSyncDatabase::class.java,
            "foldersync.db",
        ).build()
    }

    @Provides
    fun provideSyncProfileDao(db: FolderSyncDatabase): SyncProfileDao = db.syncProfileDao()

    @Provides
    fun provideSyncRunDao(db: FolderSyncDatabase): SyncRunDao = db.syncRunDao()

    @Provides
    fun provideFileMetadataDao(db: FolderSyncDatabase): FileMetadataDao = db.fileMetadataDao()
}