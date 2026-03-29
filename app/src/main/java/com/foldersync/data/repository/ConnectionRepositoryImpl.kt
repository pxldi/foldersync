package com.foldersync.data.repository

import com.foldersync.data.db.dao.ConnectionDao
import com.foldersync.data.db.entity.ConnectionEntity
import com.foldersync.domain.repository.ConnectionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectionRepositoryImpl @Inject constructor(
    private val dao: ConnectionDao,
) : ConnectionRepository {
    override fun observeAll(): Flow<List<ConnectionEntity>> = dao.observeAll()
    override suspend fun getAll(): List<ConnectionEntity> = dao.getAll()
    override suspend fun getById(id: Long): ConnectionEntity? = dao.getById(id)
    override suspend fun insert(connection: ConnectionEntity): Long = dao.insert(connection)
    override suspend fun update(connection: ConnectionEntity) = dao.update(connection)
    override suspend fun delete(id: Long) = dao.deleteById(id)
}