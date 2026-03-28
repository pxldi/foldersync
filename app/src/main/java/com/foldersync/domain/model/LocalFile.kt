package com.foldersync.domain.model

import android.net.Uri

data class LocalFile(
    val uri: Uri,
    val path: String,          // relative path from sync root
    val name: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long,    // epoch millis
)