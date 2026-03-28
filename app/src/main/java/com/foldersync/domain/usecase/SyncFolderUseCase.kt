package com.foldersync.domain.usecase

import android.net.Uri
import com.foldersync.data.db.dao.FileMetadataDao
import com.foldersync.data.db.entity.SyncRunEntity
import com.foldersync.data.local.LocalFileScanner
import com.foldersync.domain.model.ConflictStrategy
import com.foldersync.domain.model.SyncDirection
import com.foldersync.domain.model.SyncStatus
import com.foldersync.domain.model.WebDavCredentials
import com.foldersync.domain.repository.SyncRunRepository
import com.foldersync.domain.repository.WebDavRepository
import com.foldersync.domain.sync.SyncDiffCalculator
import com.foldersync.domain.sync.SyncExecutor
import javax.inject.Inject

class SyncFolderUseCase @Inject constructor(
    private val localFileScanner: LocalFileScanner,
    private val webDavRepository: WebDavRepository,
    private val fileMetadataDao: FileMetadataDao,
    private val diffCalculator: SyncDiffCalculator,
    private val syncExecutor: SyncExecutor,
    private val syncRunRepository: SyncRunRepository,
) {
    data class SyncResult(
        val success: Boolean,
        val filesUploaded: Int,
        val filesDownloaded: Int,
        val filesSkipped: Int,
        val filesFailed: Int,
        val bytesTransferred: Long,
        val errors: List<String>,
    )

    suspend fun execute(
        profileId: Long,
        localUri: Uri,
        remoteUrl: String,
        credentials: WebDavCredentials,
        direction: SyncDirection,
        conflictStrategy: ConflictStrategy,
        maxDepth: Int = 0,
        onProgress: (SyncExecutor.SyncProgress) -> Unit = {},
    ): SyncResult {
        // Record that a sync run started
        val runId = syncRunRepository.insert(
            SyncRunEntity(
                profileId = profileId,
                status = SyncStatus.RUNNING,
            )
        )

        return try {
            // 1. Scan local folder
            val localFiles = localFileScanner.scanFolder(localUri, maxDepth)
                .filter { !it.isDirectory } // Only sync files, dirs are handled separately

            // 2. List remote files via PROPFIND
            val remoteFiles = webDavRepository.listFiles(remoteUrl, credentials)
                .filter { !it.isDirectory }

            // 3. Load cached metadata
            val cachedMetadata = fileMetadataDao.getByProfile(profileId)

            // 4. Calculate diff
            val actions = diffCalculator.calculateDiff(
                localFiles = localFiles,
                remoteFiles = remoteFiles,
                cachedMetadata = cachedMetadata,
                direction = direction,
                conflictStrategy = conflictStrategy,
            )

            // 5. Execute actions
            val progress = syncExecutor.execute(
                actions = actions,
                baseUrl = remoteUrl,
                credentials = credentials,
                profileId = profileId,
                onProgress = onProgress,
            )

            // 6. Record success
            val status = if (progress.failed > 0) SyncStatus.PARTIAL else SyncStatus.SUCCESS
            updateSyncRun(runId, profileId, status, progress)

            SyncResult(
                success = progress.failed == 0,
                filesUploaded = progress.uploaded,
                filesDownloaded = progress.downloaded,
                filesSkipped = progress.skipped,
                filesFailed = progress.failed,
                bytesTransferred = progress.bytesTransferred,
                errors = progress.errors,
            )
        } catch (e: Exception) {
            updateSyncRun(
                runId, profileId, SyncStatus.FAILED,
                SyncExecutor.SyncProgress(errors = listOf(e.message ?: "Unknown error")),
            )

            SyncResult(
                success = false,
                filesUploaded = 0,
                filesDownloaded = 0,
                filesSkipped = 0,
                filesFailed = 0,
                bytesTransferred = 0,
                errors = listOf(e.message ?: "Unknown error"),
            )
        }
    }

    private suspend fun updateSyncRun(
        runId: Long,
        profileId: Long,
        status: SyncStatus,
        progress: SyncExecutor.SyncProgress,
    ) {
        val run = SyncRunEntity(
            id = runId,
            profileId = profileId,
            finishedAt = System.currentTimeMillis(),
            status = status,
            filesUploaded = progress.uploaded,
            filesDownloaded = progress.downloaded,
            filesSkipped = progress.skipped,
            filesFailed = progress.failed,
            bytesTransferred = progress.bytesTransferred,
            errorMessage = progress.errors.firstOrNull(),
        )
        syncRunRepository.update(run)
    }
}