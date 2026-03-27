package com.foldersync.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.foldersync.data.db.entity.SyncProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncProfileDao {

    @Query("SELECT * FROM sync_profiles ORDER BY name ASC")
    fun observeAll(): Flow<List<SyncProfileEntity>>

    @Query("SELECT * FROM sync_profiles")
    suspend fun getAll(): List<SyncProfileEntity>

    @Query("SELECT * FROM sync_profiles WHERE id = :id")
    suspend fun getById(id: Long): SyncProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: SyncProfileEntity): Long

    @Update
    suspend fun update(profile: SyncProfileEntity)

    @Query("DELETE FROM sync_profiles WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Delete
    suspend fun delete(profile: SyncProfileEntity)

    @Query("SELECT * FROM sync_profiles WHERE enabled = 1")
    suspend fun getEnabled(): List<SyncProfileEntity>
}