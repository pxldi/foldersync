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
        // Basic validation before hitting the network
        if (url.isBlank()) {
            return ConnectionTestResult.Failure("URL cannot be empty")
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return ConnectionTestResult.Failure("URL must start with http:// or https://")
        }
        if (credentials.username.isBlank()) {
            return ConnectionTestResult.Failure("Username cannot be empty")
        }

        return webDavRepository.testConnection(url, credentials)
    }
}