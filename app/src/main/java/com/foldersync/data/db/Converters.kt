package com.foldersync.data.db

import androidx.room.TypeConverter
import com.foldersync.domain.model.ConflictStrategy
import com.foldersync.domain.model.SyncDirection
import com.foldersync.domain.model.SyncStatus

class Converters {
    // SyncDirection
    @TypeConverter
    fun fromSyncDirection(value: SyncDirection): String = value.name

    @TypeConverter
    fun toSyncDirection(value: String): SyncDirection = SyncDirection.valueOf(value)

    // ConflictStrategy
    @TypeConverter
    fun fromConflictStrategy(value: ConflictStrategy): String = value.name

    @TypeConverter
    fun toConflictStrategy(value: String): ConflictStrategy = ConflictStrategy.valueOf(value)

    // SyncStatus
    @TypeConverter
    fun fromSyncStatus(value: SyncStatus): String = value.name

    @TypeConverter
    fun toSyncStatus(value: String): SyncStatus = SyncStatus.valueOf(value)
}