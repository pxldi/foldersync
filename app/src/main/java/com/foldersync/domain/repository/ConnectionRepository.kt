package com.foldersync.domain.repository

import com.foldersync.data.db.entity.ConnectionEntity
import kotlinx.coroutines.flow.Flow

interface ConnectionRepository {
    fun observeAll(): Flow<List<ConnectionEntity>>
    suspend fun getAll(): List<ConnectionEntity>
    suspend fun getById(id: Long): ConnectionEntity?
    suspend fun insert(connection: ConnectionEntity): Long
    suspend fun update(connection: ConnectionEntity)
    suspend fun delete(id: Long)
}