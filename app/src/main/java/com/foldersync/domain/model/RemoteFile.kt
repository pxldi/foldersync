package com.foldersync.domain.model

/**
 * Represents a file or directory on the remote WebDAV server.
 * This is what PROPFIND returns for each resource.
 */
data class RemoteFile(
    val path: String,              // relative path from sync root
    val name: String,              // display name
    val isDirectory: Boolean,
    val size: Long,                // bytes, 0 for directories
    val lastModified: Long,        // epoch millis
    val etag: String?,             // server ETag, null if not provided
    val contentType: String?,      // MIME type, null for directories
)