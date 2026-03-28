package com.foldersync.domain.repository

import com.foldersync.data.db.entity.SyncRunEntity
import kotlinx.coroutines.flow.Flow

interface SyncRunRepository {
    fun observeByProfile(profileId: Long): Flow<List<SyncRunEntity>>
    suspend fun getLatestByProfile(profileId: Long): SyncRunEntity?
    suspend fun insert(run: SyncRunEntity): Long
    suspend fun update(run: SyncRunEntity)
    suspend fun deleteByProfile(profileId: Long)
}