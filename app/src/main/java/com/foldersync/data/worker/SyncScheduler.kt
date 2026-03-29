package com.foldersync.data.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.foldersync.data.db.entity.SyncProfileEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val workManager = WorkManager.getInstance(context)

    /**
     * Schedule periodic sync for a profile.
     * Minimum interval is 15 minutes (WorkManager hard limit).
     */
    fun schedule(profile: SyncProfileEntity) {
        if (profile.scheduledHour != null) {
            scheduleDailyAt(profile, profile.scheduledHour, profile.scheduledMinute)
        } else {
            scheduleInterval(profile)
        }
    }

    private fun scheduleDailyAt(profile: SyncProfileEntity, hour: Int, minute: Int) {
        // Calculate delay until next occurrence of this time
        val now = java.util.Calendar.getInstance()
        val target = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, hour)
            set(java.util.Calendar.MINUTE, minute)
            set(java.util.Calendar.SECOND, 0)
            if (before(now)) add(java.util.Calendar.DAY_OF_MONTH, 1)
        }
        val initialDelay = target.timeInMillis - now.timeInMillis

        val constraints = buildConstraints(profile)

        val request = PeriodicWorkRequestBuilder<SyncWorker>(
            24, TimeUnit.HOURS,
        )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .setInputData(workDataOf(SyncWorker.KEY_PROFILE_ID to profile.id))
            .addTag("sync_profile_${profile.id}")
            .build()

        workManager.enqueueUniquePeriodicWork(
            uniqueWorkName(profile.id),
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    private fun scheduleInterval(profile: SyncProfileEntity) {
        val interval = profile.intervalMinutes.toLong().coerceAtLeast(15)
        val constraints = buildConstraints(profile)

        val request = PeriodicWorkRequestBuilder<SyncWorker>(
            interval, TimeUnit.MINUTES,
        )
            .setConstraints(constraints)
            .setInputData(workDataOf(SyncWorker.KEY_PROFILE_ID to profile.id))
            .addTag("sync_profile_${profile.id}")
            .build()

        workManager.enqueueUniquePeriodicWork(
            uniqueWorkName(profile.id),
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    private fun buildConstraints(profile: SyncProfileEntity) = Constraints.Builder()
        .setRequiredNetworkType(
            if (profile.wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED
        )
        .setRequiresBatteryNotLow(true)
        .setRequiresCharging(profile.chargingOnly)
        .build()

    /**
     * Trigger an immediate one-time sync for a profile.
     */
    fun syncNow(profileId: Long) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setInputData(
                workDataOf(SyncWorker.KEY_PROFILE_ID to profileId)
            )
            .addTag("sync_now_${profileId}")
            .build()

        workManager.enqueue(request)
    }

    /**
     * Cancel scheduled sync for a profile.
     */
    fun cancel(profileId: Long) {
        workManager.cancelUniqueWork(uniqueWorkName(profileId))
    }

    private fun uniqueWorkName(profileId: Long) = "sync_periodic_$profileId"
}