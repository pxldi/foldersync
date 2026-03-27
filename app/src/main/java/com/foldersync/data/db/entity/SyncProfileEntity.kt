package com.foldersync.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.foldersync.domain.model.ConflictStrategy
import com.foldersync.domain.model.SyncDirection

@Entity(tableName = "sync_profiles")
data class SyncProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String,

    @ColumnInfo(name = "local_uri")
    val localUri: String,

    @ColumnInfo(name = "remote_url")
    val remoteUrl: String,

    @ColumnInfo(name = "credential_ref")
    val credentialRef: String,

    val direction: SyncDirection,

    @ColumnInfo(name = "conflict_strategy")
    val conflictStrategy: ConflictStrategy,

    @ColumnInfo(name = "interval_minutes")
    val intervalMinutes: Int = 60,

    @ColumnInfo(name = "wifi_only")
    val wifiOnly: Boolean = true,

    @ColumnInfo(name = "charging_only")
    val chargingOnly: Boolean = false,

    val enabled: Boolean = false,

    @ColumnInfo(name = "include_pattern")
    val includePattern: String? = null,

    @ColumnInfo(name = "exclude_pattern")
    val excludePattern: String? = null,

    @ColumnInfo(name = "max_depth")
    val maxDepth: Int = 0,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
)