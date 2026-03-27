package com.foldersync.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.foldersync.data.db.dao.FileMetadataDao
import com.foldersync.data.db.dao.SyncProfileDao
import com.foldersync.data.db.dao.SyncRunDao
import com.foldersync.data.db.entity.FileMetadataEntity
import com.foldersync.data.db.entity.SyncProfileEntity
import com.foldersync.data.db.entity.SyncRunEntity

@Database(
    entities = [
        SyncProfileEntity::class,
        SyncRunEntity::class,
        FileMetadataEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class FolderSyncDatabase : RoomDatabase() {
    abstract fun syncProfileDao(): SyncProfileDao
    abstract fun syncRunDao(): SyncRunDao
    abstract fun fileMetadataDao(): FileMetadataDao
}