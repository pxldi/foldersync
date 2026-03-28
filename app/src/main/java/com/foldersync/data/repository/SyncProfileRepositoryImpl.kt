package com.foldersync.data.repository

import com.foldersync.data.db.dao.SyncProfileDao
import com.foldersync.data.db.entity.SyncProfileEntity
import com.foldersync.domain.repository.SyncProfileRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncProfileRepositoryImpl @Inject constructor(
    private val dao: SyncProfileDao,
) : SyncProfileRepository {

    override fun observeAll(): Flow<List<SyncProfileEntity>> = dao.observeAll()

    override suspend fun getAll(): List<SyncProfileEntity> = dao.getAll()

    override suspend fun getById(id: Long): SyncProfileEntity? = dao.getById(id)

    override suspend fun insert(profile: SyncProfileEntity): Long = dao.insert(profile)

    override suspend fun update(profile: SyncProfileEntity) = dao.update(profile)

    override suspend fun delete(id: Long) = dao.deleteById(id)

    override suspend fun getEnabled(): List<SyncProfileEntity> = dao.getEnabled()
}