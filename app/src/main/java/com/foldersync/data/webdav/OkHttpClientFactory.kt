package com.foldersync.data.webdav

import at.bitfire.dav4jvm.BasicDigestAuthHandler
import com.foldersync.domain.model.WebDavCredentials
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OkHttpClientFactory @Inject constructor() {

    // Cache clients per credential set to reuse connection pools
    private val cache = mutableMapOf<String, OkHttpClient>()

    fun create(credentials: WebDavCredentials): OkHttpClient {
        val key = "${credentials.username}:${credentials.password.hashCode()}"

        return cache.getOrPut(key) {
            val authHandler = BasicDigestAuthHandler(
                domain = null,
                username = credentials.username,
                password = credentials.password,
            )

            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

            OkHttpClient.Builder()
                .followRedirects(false)
                .authenticator(authHandler)
                .addNetworkInterceptor(authHandler)
                .addInterceptor(logging)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build()
        }
    }
}