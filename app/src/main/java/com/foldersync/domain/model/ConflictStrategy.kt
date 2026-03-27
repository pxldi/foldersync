package com.foldersync.domain.model

enum class ConflictStrategy {
    LOCAL_WINS,
    REMOTE_WINS,
    KEEP_BOTH,
    NEWEST_WINS,
}