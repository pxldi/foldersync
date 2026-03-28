package com.foldersync.data.local

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.foldersync.domain.model.LocalFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.emptyList

@Singleton
class LocalFileScanner @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /**
     * Recursively scans a SAF tree URI and returns a flat list of files.
     * Directories are included in the list (isDirectory = true).
     *
     * @param treeUri  The SAF tree URI from ACTION_OPEN_DOCUMENT_TREE
     * @param maxDepth 0 = unlimited, otherwise stop recursing at this depth
     */
    suspend fun scanFolder(
        treeUri: Uri,
        maxDepth: Int = 0,
    ): List<LocalFile> = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(context, treeUri)
            ?: return@withContext emptyList()

        val results = mutableListOf<LocalFile>()
        scanRecursive(root, "", results, currentDepth = 1, maxDepth = maxDepth)
        results
    }

    private fun scanRecursive(
        dir: DocumentFile,
        parentPath: String,
        results: MutableList<LocalFile>,
        currentDepth: Int,
        maxDepth: Int,
    ) {
        val children = dir.listFiles()

        for (child in children) {
            val name = child.name ?: continue
            val relativePath = if (parentPath.isEmpty()) name else "$parentPath/$name"

            results.add(
                LocalFile(
                    uri = child.uri,
                    path = relativePath,
                    name = name,
                    isDirectory = child.isDirectory,
                    size = if (child.isFile) child.length() else 0L,
                    lastModified = child.lastModified(),
                )
            )

            // Recurse into subdirectories
            if (child.isDirectory && (maxDepth == 0 || currentDepth < maxDepth)) {
                scanRecursive(child, relativePath, results, currentDepth + 1, maxDepth)
            }
        }
    }

    /**
     * Reads the raw bytes of a file from its SAF URI.
     * For small-to-medium files only — large files should stream.
     */
    suspend fun readFileBytes(uri: Uri): ByteArray? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Gets the MIME type of a file from its SAF URI.
     */
    fun getMimeType(uri: Uri): String {
        return context.contentResolver.getType(uri) ?: "application/octet-stream"
    }
}