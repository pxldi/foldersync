package com.foldersync.data.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat

object SyncNotificationHelper {

    const val CHANNEL_ID = "foldersync_sync"
    const val NOTIFICATION_ID = 1001

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Folder Sync",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Shows progress during folder synchronization"
        }

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    fun buildProgressNotification(
        context: Context,
        profileName: String,
        progress: Int,
        total: Int,
    ): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setContentTitle("Syncing: $profileName")
            .setContentText("$progress / $total files")
            .setProgress(total, progress, total == 0)
            .setOngoing(true)
            .setSilent(true)
    }

    fun buildCompleteNotification(
        context: Context,
        profileName: String,
        uploaded: Int,
        downloaded: Int,
        failed: Int,
    ): NotificationCompat.Builder {
        val text = buildString {
            if (uploaded > 0) append("Uploaded: $uploaded ")
            if (downloaded > 0) append("Downloaded: $downloaded ")
            if (failed > 0) append("Failed: $failed")
            if (isEmpty()) append("Everything up to date")
        }

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(
                if (failed > 0) android.R.drawable.ic_dialog_alert
                else android.R.drawable.ic_dialog_info
            )
            .setContentTitle("Sync complete: $profileName")
            .setContentText(text.trim())
            .setAutoCancel(true)
    }
}