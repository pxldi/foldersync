package com.foldersync.di

import com.foldersync.data.repository.ConnectionRepositoryImpl
import com.foldersync.data.repository.SyncProfileRepositoryImpl
import com.foldersync.data.repository.SyncRunRepositoryImpl
import com.foldersync.data.repository.WebDavRepositoryImpl
import com.foldersync.domain.repository.ConnectionRepository
import com.foldersync.domain.repository.SyncProfileRepository
import com.foldersync.domain.repository.SyncRunRepository
import com.foldersync.domain.repository.WebDavRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSyncProfileRepository(
        impl: SyncProfileRepositoryImpl,
    ): SyncProfileRepository

    @Binds
    @Singleton
    abstract fun bindSyncRunRepository(
        impl: SyncRunRepositoryImpl,
    ): SyncRunRepository

    @Binds
    @Singleton
    abstract fun bindWebDavRepository(
        impl: WebDavRepositoryImpl,
    ): WebDavRepository

    @Binds
    @Singleton
    abstract fun bindConnectionRepository(
        impl: ConnectionRepositoryImpl,
    ): ConnectionRepository
}