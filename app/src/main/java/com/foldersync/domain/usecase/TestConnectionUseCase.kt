package com.foldersync.domain.usecase

import com.foldersync.domain.model.ConnectionTestResult
import com.foldersync.domain.model.WebDavCredentials
import com.foldersync.domain.repository.WebDavRepository
import javax.inject.Inject

class TestConnectionUseCase @Inject constructor(
    private val webDavRepository: WebDavRepository,
) {
    suspend operator fun invoke(
        url: String,
        credentials: WebDavCredentials,
    ): ConnectionTestResult {
        // Basic validation
        if (url.isBlank()) {
            return ConnectionTestResult.Failure("URL cannot be empty")
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return ConnectionTestResult.Failure("URL must start with http:// or https://")
        }
        if (credentials.username.isBlank()) {
            return ConnectionTestResult.Failure("Username cannot be empty")
        }

        // Try connection
        val result = webDavRepository.testConnection(url, credentials)

        // If it failed, try creating the directory and test again
        if (result is ConnectionTestResult.Failure && result.message.contains("404")) {
            val mkdirResult = webDavRepository.createDirectory(url, credentials)
            if (mkdirResult) {
                // Retry the test after creating the directory
                return webDavRepository.testConnection(url, credentials)
            }
        }

        return result
    }
}