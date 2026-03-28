package com.foldersync.domain.repository

import com.foldersync.data.db.entity.SyncProfileEntity
import kotlinx.coroutines.flow.Flow

interface SyncProfileRepository {
    fun observeAll(): Flow<List<SyncProfileEntity>>
    suspend fun getAll(): List<SyncProfileEntity>
    suspend fun getById(id: Long): SyncProfileEntity?
    suspend fun insert(profile: SyncProfileEntity): Long
    suspend fun update(profile: SyncProfileEntity)
    suspend fun delete(id: Long)
    suspend fun getEnabled(): List<SyncProfileEntity>
}