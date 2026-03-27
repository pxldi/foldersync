package com.foldersync.domain.model

enum class SyncStatus {
    NEVER,
    RUNNING,
    SUCCESS,
    PARTIAL,
    FAILED,
    CANCELLED,
}