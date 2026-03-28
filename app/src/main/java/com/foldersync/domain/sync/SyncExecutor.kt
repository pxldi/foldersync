package com.foldersync.domain.sync

import android.util.Log
import com.foldersync.data.db.dao.FileMetadataDao
import com.foldersync.data.db.entity.FileMetadataEntity
import com.foldersync.data.local.LocalFileScanner
import com.foldersync.domain.model.SyncAction
import com.foldersync.domain.model.WebDavCredentials
import com.foldersync.domain.repository.WebDavRepository
import javax.inject.Inject

/**
 * Executes a list of sync actions and tracks progress.
 */
class SyncExecutor @Inject constructor(
    private val webDavRepository: WebDavRepository,
    private val localFileScanner: LocalFileScanner,
    private val fileMetadataDao: FileMetadataDao,
) {
    data class SyncProgress(
        val total: Int = 0,
        val completed: Int = 0,
        val uploaded: Int = 0,
        val downloaded: Int = 0,
        val skipped: Int = 0,
        val failed: Int = 0,
        val bytesTransferred: Long = 0,
        val errors: List<String> = emptyList(),
    )

    /**
     * Execute all sync actions against the remote server.
     *
     * @param actions   List of actions from SyncDiffCalculator
     * @param baseUrl   The WebDAV base URL
     * @param credentials  WebDAV credentials
     * @param profileId    Profile ID for updating metadata cache
     * @param onProgress   Callback for progress updates
     */
    suspend fun execute(
        actions: List<SyncAction>,
        baseUrl: String,
        credentials: WebDavCredentials,
        profileId: Long,
        onProgress: (SyncProgress) -> Unit = {},
    ): SyncProgress {
        var progress = SyncProgress(total = actions.size)
        onProgress(progress)

        val errors = mutableListOf<String>()

        for (action in actions) {
            try {
                when (action) {
                    is SyncAction.Upload -> {
                        val bytes = localFileScanner.readFileBytes(action.localUri)
                        if (bytes != null) {
                            val fileUrl = ensureTrailingSlash(baseUrl) + action.relativePath
                            val success = webDavRepository.uploadFile(
                                fileUrl = fileUrl,
                                credentials = credentials,
                                bytes = bytes,
                                contentType = action.contentType,
                            )
                            if (success) {
                                progress = progress.copy(
                                    uploaded = progress.uploaded + 1,
                                    bytesTransferred = progress.bytesTransferred + bytes.size,
                                )
                                // Update metadata cache
                                updateMetadataAfterUpload(profileId, action.relativePath, bytes)
                            } else {
                                errors.add("Upload failed: ${action.relativePath}")
                                progress = progress.copy(failed = progress.failed + 1)
                            }
                        } else {
                            errors.add("Could not read local file: ${action.relativePath}")
                            progress = progress.copy(failed = progress.failed + 1)
                        }
                    }

                    is SyncAction.Download -> {
                        val fileUrl = ensureTrailingSlash(baseUrl) + action.relativePath
                        val bytes = webDavRepository.downloadFile(fileUrl, credentials)
                        if (bytes != null) {
                            // TODO: Write bytes to local SAF storage
                            // For now, just count it as downloaded
                            progress = progress.copy(
                                downloaded = progress.downloaded + 1,
                                bytesTransferred = progress.bytesTransferred + bytes.size,
                            )
                            Log.d("SyncExecutor", "Downloaded: ${action.relativePath} (${bytes.size} bytes)")
                        } else {
                            errors.add("Download failed: ${action.relativePath}")
                            progress = progress.copy(failed = progress.failed + 1)
                        }
                    }

                    is SyncAction.CreateRemoteDir -> {
                        val dirUrl = ensureTrailingSlash(baseUrl) + action.relativePath
                        webDavRepository.createDirectory(dirUrl, credentials)
                        // Dirs don't count toward upload/download totals
                    }

                    is SyncAction.DeleteRemote -> {
                        val fileUrl = ensureTrailingSlash(baseUrl) + action.relativePath
                        webDavRepository.delete(fileUrl, credentials)
                        fileMetadataDao.delete(profileId, action.relativePath)
                    }

                    is SyncAction.DeleteLocal -> {
                        // TODO: Delete via SAF DocumentFile
                        fileMetadataDao.delete(profileId, action.relativePath)
                    }

                    is SyncAction.Skip -> {
                        progress = progress.copy(skipped = progress.skipped + 1)
                    }
                }
            } catch (e: Exception) {
                errors.add("${action.relativePath}: ${e.message}")
                progress = progress.copy(failed = progress.failed + 1)
            }

            progress = progress.copy(
                completed = progress.completed + 1,
                errors = errors.toList(),
            )
            onProgress(progress)
        }

        return progress
    }

    private suspend fun updateMetadataAfterUpload(
        profileId: Long,
        relativePath: String,
        bytes: ByteArray,
    ) {
        fileMetadataDao.upsert(
            FileMetadataEntity(
                profileId = profileId,
                relativePath = relativePath,
                localETag = bytes.size.toString(), // Simple size-based "ETag" for now
                remoteETag = null, // Will be updated on next PROPFIND
                lastSyncedAt = System.currentTimeMillis(),
            )
        )
    }

    private fun ensureTrailingSlash(url: String): String =
        if (url.endsWith("/")) url else "$url/"
}