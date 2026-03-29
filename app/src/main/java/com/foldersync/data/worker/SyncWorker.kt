package com.foldersync.data.worker

import android.content.Context
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.foldersync.data.prefs.CredentialStore
import com.foldersync.domain.repository.SyncProfileRepository
import com.foldersync.domain.usecase.SyncFolderUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Background worker that syncs a single profile.
 *
 * Runs as a foreground service with a progress notification.
 * WorkManager guarantees execution even after app kill or reboot.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val params: WorkerParameters,
    private val syncProfileRepo: SyncProfileRepository,
    private val credentialStore: CredentialStore,
    private val syncFolder: SyncFolderUseCase,
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_PROFILE_ID = "profile_id"
        private const val TAG = "SyncWorker"
    }

    override suspend fun doWork(): Result {
        val profileId = inputData.getLong(KEY_PROFILE_ID, -1)
        if (profileId == -1L) {
            Log.e(TAG, "No profile ID provided")
            return Result.failure()
        }

        val profile = syncProfileRepo.getById(profileId)
        if (profile == null) {
            Log.e(TAG, "Profile $profileId not found")
            return Result.failure()
        }

        val credentials = credentialStore.get(profile.credentialRef)
        if (credentials == null) {
            Log.e(TAG, "No credentials for profile ${profile.name}")
            return Result.failure()
        }

        // Create notification channel
        SyncNotificationHelper.createChannel(context)

        // Start as foreground service with progress notification
        setForeground(createForegroundInfo(profile.name, 0, 0))

        Log.i(TAG, "Starting sync for profile: ${profile.name}")

        val result = syncFolder.execute(
            profileId = profileId,
            localUri = Uri.parse(profile.localUri),
            remoteUrl = profile.remoteUrl,
            credentials = credentials,
            direction = profile.direction,
            conflictStrategy = profile.conflictStrategy,
            maxDepth = profile.maxDepth,
            onProgress = { progress ->
                // Update the notification with current progress
                try {
                    setForegroundAsync(
                        createForegroundInfo(
                            profile.name,
                            progress.completed,
                            progress.total,
                        )
                    )
                } catch (e: Exception) {
                    // Notification update can fail if worker is stopping
                }
            },
        )

        // Show completion notification
        showCompletionNotification(
            profile.name,
            result.filesUploaded,
            result.filesDownloaded,
            result.filesFailed,
        )

        Log.i(TAG, "Sync complete for ${profile.name}: " +
                "uploaded=${result.filesUploaded}, " +
                "downloaded=${result.filesDownloaded}, " +
                "failed=${result.filesFailed}")

        return if (result.success) Result.success() else Result.retry()
    }

    private fun createForegroundInfo(
        profileName: String,
        progress: Int,
        total: Int,
    ): ForegroundInfo {
        val notification = SyncNotificationHelper.buildProgressNotification(
            context, profileName, progress, total,
        ).build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                SyncNotificationHelper.NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            ForegroundInfo(SyncNotificationHelper.NOTIFICATION_ID, notification)
        }
    }

    private fun showCompletionNotification(
        profileName: String,
        uploaded: Int,
        downloaded: Int,
        failed: Int,
    ) {
        val notification = SyncNotificationHelper.buildCompleteNotification(
            context, profileName, uploaded, downloaded, failed,
        ).build()

        try {
            NotificationManagerCompat.from(context)
                .notify(SyncNotificationHelper.NOTIFICATION_ID + 1, notification)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS permission not granted — silently skip
            Log.w(TAG, "Cannot show notification: ${e.message}")
        }
    }
}