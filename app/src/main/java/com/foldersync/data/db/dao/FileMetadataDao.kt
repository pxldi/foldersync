package com.foldersync.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.foldersync.data.db.entity.FileMetadataEntity

@Dao
interface FileMetadataDao {

    @Query("SELECT * FROM file_metadata WHERE profile_id = :profileId")
    suspend fun getByProfile(profileId: Long): List<FileMetadataEntity>

    @Query("SELECT * FROM file_metadata WHERE profile_id = :profileId AND relative_path = :path")
    suspend fun get(profileId: Long, path: String): FileMetadataEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(metadata: FileMetadataEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(metadata: List<FileMetadataEntity>)

    @Query("DELETE FROM file_metadata WHERE profile_id = :profileId AND relative_path = :path")
    suspend fun delete(profileId: Long, path: String)

    @Query("DELETE FROM file_metadata WHERE profile_id = :profileId")
    suspend fun deleteByProfile(profileId: Long)
}