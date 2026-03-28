package com.foldersync.domain.sync

import com.foldersync.data.db.entity.FileMetadataEntity
import com.foldersync.domain.model.ConflictStrategy
import com.foldersync.domain.model.LocalFile
import com.foldersync.domain.model.RemoteFile
import com.foldersync.domain.model.SyncAction
import com.foldersync.domain.model.SyncDirection
import javax.inject.Inject

/**
 * Pure logic — no I/O, no Android dependencies.
 * Compares local files, remote files, and cached metadata
 * to produce a list of sync actions.
 */
class SyncDiffCalculator @Inject constructor() {

    fun calculateDiff(
        localFiles: List<LocalFile>,
        remoteFiles: List<RemoteFile>,
        cachedMetadata: List<FileMetadataEntity>,
        direction: SyncDirection,
        conflictStrategy: ConflictStrategy,
    ): List<SyncAction> {
        val localByPath = localFiles.associateBy { it.path }
        val remoteByPath = remoteFiles.associateBy { it.path }
        val cacheByPath = cachedMetadata.associateBy { it.relativePath }

        val allPaths = (localByPath.keys + remoteByPath.keys).distinct()
        val actions = mutableListOf<SyncAction>()

        for (path in allPaths) {
            val local = localByPath[path]
            val remote = remoteByPath[path]
            val cached = cacheByPath[path]

            val action = calculateAction(local, remote, cached, direction, conflictStrategy)
            if (action != null) {
                actions.add(action)
            }
        }

        // Sort: directories first (so they get created before files inside them),
        // then alphabetically
        return actions.sortedWith(compareBy<SyncAction> {
            when (it) {
                is SyncAction.CreateRemoteDir -> 0
                else -> 1
            }
        }.thenBy { it.relativePath })
    }

    private fun calculateAction(
        local: LocalFile?,
        remote: RemoteFile?,
        cached: FileMetadataEntity?,
        direction: SyncDirection,
        conflictStrategy: ConflictStrategy,
    ): SyncAction? {
        return when {
            // ── EXISTS LOCALLY ONLY ──
            local != null && remote == null -> {
                if (direction == SyncDirection.DOWNLOAD) {
                    // Download-only mode: local-only file means it was deleted
                    // remotely (or never existed). If it was in cache, delete local.
                    if (cached != null) {
                        SyncAction.DeleteLocal(local.path, local.uri)
                    } else {
                        SyncAction.Skip(local.path, "Local-only in download mode")
                    }
                } else {
                    // Upload or bidirectional: push to remote
                    if (local.isDirectory) {
                        SyncAction.CreateRemoteDir(local.path)
                    } else {
                        SyncAction.Upload(
                            relativePath = local.path,
                            localUri = local.uri,
                            contentType = "application/octet-stream",
                        )
                    }
                }
            }

            // ── EXISTS REMOTELY ONLY ──
            local == null && remote != null -> {
                if (direction == SyncDirection.UPLOAD) {
                    // Upload-only mode: remote-only means deleted locally
                    if (cached != null) {
                        SyncAction.DeleteRemote(remote.path, "")
                    } else {
                        SyncAction.Skip(remote.path, "Remote-only in upload mode")
                    }
                } else {
                    // Download or bidirectional
                    if (remote.isDirectory) {
                        // Directories are created on-demand when downloading files
                        SyncAction.Skip(remote.path, "Remote directory")
                    } else {
                        SyncAction.Download(
                            relativePath = remote.path,
                            remoteUrl = "",  // Caller fills in the full URL
                        )
                    }
                }
            }

            // ── EXISTS BOTH SIDES ──
            local != null && remote != null -> {
                if (local.isDirectory || remote.isDirectory) {
                    // Both are directories — nothing to sync
                    return null
                }

                val localChanged = cached == null ||
                        local.lastModified.toString() != cached.localETag
                val remoteChanged = cached == null ||
                        remote.etag != cached.remoteETag

                when {
                    !localChanged && !remoteChanged -> {
                        // Nothing changed — skip
                        null
                    }
                    localChanged && !remoteChanged -> {
                        // Only local changed → upload
                        if (direction == SyncDirection.DOWNLOAD) null
                        else SyncAction.Upload(local.path, local.uri, "application/octet-stream")
                    }
                    !localChanged && remoteChanged -> {
                        // Only remote changed → download
                        if (direction == SyncDirection.UPLOAD) null
                        else SyncAction.Download(remote.path, "")
                    }
                    else -> {
                        // Both changed → conflict!
                        resolveConflict(local, remote, conflictStrategy, direction)
                    }
                }
            }

            else -> null
        }
    }

    private fun resolveConflict(
        local: LocalFile,
        remote: RemoteFile,
        strategy: ConflictStrategy,
        direction: SyncDirection,
    ): SyncAction {
        return when (strategy) {
            ConflictStrategy.LOCAL_WINS -> {
                SyncAction.Upload(local.path, local.uri, "application/octet-stream")
            }
            ConflictStrategy.REMOTE_WINS -> {
                SyncAction.Download(remote.path, "")
            }
            ConflictStrategy.NEWEST_WINS -> {
                if (local.lastModified >= remote.lastModified) {
                    SyncAction.Upload(local.path, local.uri, "application/octet-stream")
                } else {
                    SyncAction.Download(remote.path, "")
                }
            }
            ConflictStrategy.KEEP_BOTH -> {
                // In keep-both mode, we upload the local with a renamed path
                // The caller handles the rename logic
                SyncAction.Upload(
                    relativePath = addConflictSuffix(local.path),
                    localUri = local.uri,
                    contentType = "application/octet-stream",
                )
            }
        }
    }

    private fun addConflictSuffix(path: String): String {
        val dot = path.lastIndexOf('.')
        return if (dot > 0) {
            val name = path.substring(0, dot)
            val ext = path.substring(dot)
            "${name}_conflict_${System.currentTimeMillis()}$ext"
        } else {
            "${path}_conflict_${System.currentTimeMillis()}"
        }
    }
}