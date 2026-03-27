package com.foldersync.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "file_metadata",
    primaryKeys = ["profile_id", "relative_path"],
    foreignKeys = [
        ForeignKey(
            entity = SyncProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profile_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("profile_id")],
)
data class FileMetadataEntity(
    @ColumnInfo(name = "profile_id")
    val profileId: Long,

    @ColumnInfo(name = "relative_path")
    val relativePath: String,

    @ColumnInfo(name = "local_etag")
    val localETag: String? = null,

    @ColumnInfo(name = "remote_etag")
    val remoteETag: String? = null,

    @ColumnInfo(name = "last_synced_at")
    val lastSyncedAt: Long = System.currentTimeMillis(),
)