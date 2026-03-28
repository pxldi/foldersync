package com.foldersync.domain.model

import android.net.Uri

/**
 * Represents a single action the sync engine needs to perform.
 */
sealed class SyncAction {
    abstract val relativePath: String

    data class Upload(
        override val relativePath: String,
        val localUri: Uri,
        val contentType: String,
    ) : SyncAction()

    data class Download(
        override val relativePath: String,
        val remoteUrl: String,
    ) : SyncAction()

    data class DeleteRemote(
        override val relativePath: String,
        val remoteUrl: String,
    ) : SyncAction()

    data class DeleteLocal(
        override val relativePath: String,
        val localUri: Uri,
    ) : SyncAction()

    data class CreateRemoteDir(
        override val relativePath: String,
    ) : SyncAction()

    data class Skip(
        override val relativePath: String,
        val reason: String,
    ) : SyncAction()
}