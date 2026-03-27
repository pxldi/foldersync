package com.foldersync.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.foldersync.data.db.entity.SyncRunEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncRunDao {

    @Query("SELECT * FROM sync_runs WHERE profile_id = :profileId ORDER BY started_at DESC")
    fun observeByProfile(profileId: Long): Flow<List<SyncRunEntity>>

    @Query("SELECT * FROM sync_runs WHERE profile_id = :profileId ORDER BY started_at DESC LIMIT 1")
    suspend fun getLatestByProfile(profileId: Long): SyncRunEntity?

    @Insert
    suspend fun insert(run: SyncRunEntity): Long

    @Update
    suspend fun update(run: SyncRunEntity)

    @Query("DELETE FROM sync_runs WHERE profile_id = :profileId")
    suspend fun deleteByProfile(profileId: Long)
}