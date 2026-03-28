package com.foldersync.data.repository

import com.foldersync.data.db.dao.SyncRunDao
import com.foldersync.data.db.entity.SyncRunEntity
import com.foldersync.domain.repository.SyncRunRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRunRepositoryImpl @Inject constructor(
    private val dao: SyncRunDao,
) : SyncRunRepository {

    override fun observeByProfile(profileId: Long): Flow<List<SyncRunEntity>> =
        dao.observeByProfile(profileId)

    override suspend fun getLatestByProfile(profileId: Long): SyncRunEntity? =
        dao.getLatestByProfile(profileId)

    override suspend fun insert(run: SyncRunEntity): Long = dao.insert(run)

    override suspend fun update(run: SyncRunEntity) = dao.update(run)

    override suspend fun deleteByProfile(profileId: Long) = dao.deleteByProfile(profileId)
}