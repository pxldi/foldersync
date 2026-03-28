package com.foldersync.data.webdav

import at.bitfire.dav4jvm.DavCollection
import at.bitfire.dav4jvm.Response
import at.bitfire.dav4jvm.property.DisplayName
import at.bitfire.dav4jvm.property.GetContentLength
import at.bitfire.dav4jvm.property.GetContentType
import at.bitfire.dav4jvm.property.GetETag
import at.bitfire.dav4jvm.property.GetLastModified
import at.bitfire.dav4jvm.property.ResourceType
import com.foldersync.domain.model.ConnectionTestResult
import com.foldersync.domain.model.RemoteFile
import com.foldersync.domain.model.WebDavCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps dav4jvm into clean suspend functions.
 *
 * dav4jvm is callback-based and uses OkHttp synchronously, so every
 * call is wrapped in withContext(Dispatchers.IO) to keep the main
 * thread free.
 */
@Singleton
class WebDavClient @Inject constructor(
    private val okHttpClientFactory: OkHttpClientFactory,
) {
    // ================================================================
    // CONNECTION TEST
    // ================================================================

    /**
     * Attempts a PROPFIND depth=0 on the given URL.
     * If it succeeds, the server is reachable and credentials are valid.
     */
    suspend fun testConnection(
        url: String,
        credentials: WebDavCredentials,
    ): ConnectionTestResult = withContext(Dispatchers.IO) {
        try {
            val client = okHttpClientFactory.create(credentials)
            val davUrl = ensureTrailingSlash(url).toHttpUrl()
            val dav = DavCollection(client, davUrl)

            var success = false
            dav.propfind(
                depth = 0,
                ResourceType.NAME,
            ) { response, relation ->
                if (relation == Response.HrefRelation.SELF) {
                    success = true
                }
            }

            if (success) {
                ConnectionTestResult.Success
            } else {
                ConnectionTestResult.Failure("Server responded but returned no data")
            }
        } catch (e: Exception) {
            ConnectionTestResult.Failure(e.message ?: "Unknown error: ${e.javaClass.simpleName}")
        }
    }

    // ================================================================
    // LIST FILES (PROPFIND depth=1)
    // ================================================================

    /**
     * Lists all files and directories in the given remote path.
     * Returns a flat list — does NOT recurse into subdirectories.
     */
    suspend fun listFiles(
        url: String,
        credentials: WebDavCredentials,
    ): List<RemoteFile> = withContext(Dispatchers.IO) {
        val client = okHttpClientFactory.create(credentials)
        val davUrl = ensureTrailingSlash(url).toHttpUrl()
        val dav = DavCollection(client, davUrl)
        val files = mutableListOf<RemoteFile>()

        dav.propfind(
            depth = 1,
            DisplayName.NAME,
            GetLastModified.NAME,
            GetContentLength.NAME,
            GetContentType.NAME,
            GetETag.NAME,
            ResourceType.NAME,
        ) { response, relation ->
            // Skip SELF — we only want children
            if (relation == Response.HrefRelation.MEMBER) {
                files.add(response.toRemoteFile(davUrl))
            }
        }

        files
    }

    // ================================================================
    // UPLOAD
    // ================================================================

    /**
     * Uploads a file to the given remote URL.
     * The URL should be the full path including filename,
     * e.g. "https://cloud.example.com/remote.php/dav/files/user/Photos/pic.jpg"
     */
    suspend fun uploadFile(
        fileUrl: String,
        credentials: WebDavCredentials,
        localFile: File,
        contentType: String = "application/octet-stream",
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val client = okHttpClientFactory.create(credentials)
            val davUrl = fileUrl.toHttpUrl()
            val dav = DavCollection(client, davUrl)

            val requestBody = localFile.asRequestBody(contentType.toMediaType())
            var success = false
            dav.put(requestBody) { response ->
                success = response.isSuccessful
            }
            success
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Uploads raw bytes (useful when reading from SAF InputStream).
     */
    suspend fun uploadBytes(
        fileUrl: String,
        credentials: WebDavCredentials,
        bytes: ByteArray,
        contentType: String = "application/octet-stream",
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val client = okHttpClientFactory.create(credentials)
            val davUrl = fileUrl.toHttpUrl()
            val dav = DavCollection(client, davUrl)

            val requestBody = bytes.toRequestBody(contentType.toMediaType())
            var success = false
            dav.put(requestBody) { response ->
                success = response.isSuccessful
            }
            success
        } catch (e: Exception) {
            false
        }
    }

    // ================================================================
    // DOWNLOAD
    // ================================================================

    /**
     * Downloads a file and returns its bytes.
     * For large files, consider streaming to disk instead.
     */
    suspend fun downloadFile(
        fileUrl: String,
        credentials: WebDavCredentials,
    ): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val client = okHttpClientFactory.create(credentials)
            val davUrl = fileUrl.toHttpUrl()
            val dav = DavCollection(client, davUrl)

            var bytes: ByteArray? = null
            dav.get(accept = "", headers = null) { response ->
                bytes = response.body?.bytes()
            }
            bytes
        } catch (e: Exception) {
            null
        }
    }

    // ================================================================
    // DELETE
    // ================================================================

    suspend fun delete(
        fileUrl: String,
        credentials: WebDavCredentials,
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val client = okHttpClientFactory.create(credentials)
            val davUrl = fileUrl.toHttpUrl()
            val dav = DavCollection(client, davUrl)

            var success = false
            dav.delete { response ->
                success = response.isSuccessful
            }
            success
        } catch (e: Exception) {
            false
        }
    }

    // ================================================================
    // CREATE DIRECTORY
    // ================================================================

    suspend fun createDirectory(
        dirUrl: String,
        credentials: WebDavCredentials,
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val client = okHttpClientFactory.create(credentials)
            val davUrl = ensureTrailingSlash(dirUrl).toHttpUrl()
            val dav = DavCollection(client, davUrl)

            var success = false
            dav.mkCol(null) { response ->
                success = response.isSuccessful
            }
            success
        } catch (e: Exception) {
            false
        }
    }

    // ================================================================
    // HELPERS
    // ================================================================

    private fun ensureTrailingSlash(url: String): String =
        if (url.endsWith("/")) url else "$url/"

    /**
     * Maps a dav4jvm Response to our domain RemoteFile model.
     */
    private fun Response.toRemoteFile(baseUrl: HttpUrl): RemoteFile {
        val props = this.properties

        val resourceType = props.filterIsInstance<ResourceType>().firstOrNull()
        val isDirectory = resourceType?.types?.contains(ResourceType.COLLECTION) == true

        val displayName = props.filterIsInstance<DisplayName>().firstOrNull()?.displayName
        val lastModified = props.filterIsInstance<GetLastModified>().firstOrNull()
            ?.lastModified ?: 0L
        val contentLength = props.filterIsInstance<GetContentLength>().firstOrNull()
            ?.contentLength ?: 0L
        val contentType = props.filterIsInstance<GetContentType>().firstOrNull()?.type?.toString()
        val etag = props.filterIsInstance<GetETag>().firstOrNull()?.eTag

        // Build relative path from the response href
        val hrefPath = this.href.encodedPath
        val basePath = baseUrl.encodedPath
        val relativePath = hrefPath.removePrefix(basePath).trimStart('/')

        val name = displayName
            ?: relativePath.trimEnd('/').substringAfterLast('/')

        return RemoteFile(
            path = relativePath,
            name = name,
            isDirectory = isDirectory,
            size = contentLength,
            lastModified = lastModified,
            etag = etag,
            contentType = contentType,
        )
    }
}