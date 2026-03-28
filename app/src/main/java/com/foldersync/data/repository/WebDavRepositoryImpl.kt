package com.foldersync.data.repository

import com.foldersync.data.webdav.WebDavClient
import com.foldersync.domain.model.ConnectionTestResult
import com.foldersync.domain.model.RemoteFile
import com.foldersync.domain.model.WebDavCredentials
import com.foldersync.domain.repository.WebDavRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebDavRepositoryImpl @Inject constructor(
    private val webDavClient: WebDavClient,
) : WebDavRepository {

    override suspend fun testConnection(
        url: String,
        credentials: WebDavCredentials,
    ): ConnectionTestResult = webDavClient.testConnection(url, credentials)

    override suspend fun listFiles(
        url: String,
        credentials: WebDavCredentials,
    ): List<RemoteFile> = webDavClient.listFiles(url, credentials)

    override suspend fun uploadFile(
        fileUrl: String,
        credentials: WebDavCredentials,
        bytes: ByteArray,
        contentType: String,
    ): Boolean = webDavClient.uploadBytes(fileUrl, credentials, bytes, contentType)

    override suspend fun downloadFile(
        fileUrl: String,
        credentials: WebDavCredentials,
    ): ByteArray? = webDavClient.downloadFile(fileUrl, credentials)

    override suspend fun delete(
        fileUrl: String,
        credentials: WebDavCredentials,
    ): Boolean = webDavClient.delete(fileUrl, credentials)

    override suspend fun createDirectory(
        dirUrl: String,
        credentials: WebDavCredentials,
    ): Boolean = webDavClient.createDirectory(dirUrl, credentials)
}