package com.foldersync.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.foldersync.domain.model.SyncStatus

@Entity(
    tableName = "sync_runs",
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
data class SyncRunEntity (
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "profile_id")
    val profileId: Long = 0,

    @ColumnInfo(name = "started_at")
    val startedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "finished_at")
    val finishedAt: Long? = null,

    val status: SyncStatus = SyncStatus.RUNNING,

    @ColumnInfo(name = "files_uploaded")
    val filesUploaded: Int = 0,

    @ColumnInfo(name = "files_downloaded")
    val filesDownloaded: Int = 0,

    @ColumnInfo(name = "files_skipped")
    val filesSkipped: Int = 0,

    @ColumnInfo(name = "files_failed")
    val filesFailed: Int = 0,

    @ColumnInfo(name = "bytes_transferred")
    val bytesTransferred: Long = 0,

    @ColumnInfo(name = "error_message")
    val errorMessage: String? = null,
)