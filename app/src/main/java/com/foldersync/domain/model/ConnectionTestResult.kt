package com.foldersync.domain.model

sealed class ConnectionTestResult {
    data object Success : ConnectionTestResult()
    data class Failure(val message: String) : ConnectionTestResult()
}