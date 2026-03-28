package com.foldersync.domain.repository

import com.foldersync.domain.model.ConnectionTestResult
import com.foldersync.domain.model.RemoteFile
import com.foldersync.domain.model.WebDavCredentials

interface WebDavRepository {
    suspend fun testConnection(url: String, credentials: WebDavCredentials): ConnectionTestResult
    suspend fun listFiles(url: String, credentials: WebDavCredentials): List<RemoteFile>
    suspend fun uploadFile(fileUrl: String, credentials: WebDavCredentials, bytes: ByteArray, contentType: String): Boolean
    suspend fun downloadFile(fileUrl: String, credentials: WebDavCredentials): ByteArray?
    suspend fun delete(fileUrl: String, credentials: WebDavCredentials): Boolean
    suspend fun createDirectory(dirUrl: String, credentials: WebDavCredentials): Boolean
}