package com.foldersync.data.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.foldersync.domain.model.WebDavCredentials
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.content.edit

/**
 * Stores WebDAV credentials encrypted using Android Keystore.
 *
 * Each profile gets its own username/password pair, keyed by a
 * credential reference string (stored in SyncProfileEntity.credentialRef).
 */
@Singleton
class CredentialStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "foldersync_credentials",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun save(credentialRef: String, credentials: WebDavCredentials) {
        prefs.edit {
            putString("${credentialRef}_username", credentials.username)
                .putString("${credentialRef}_password", credentials.password)
        }
    }

    fun get(credentialRef: String): WebDavCredentials? {
        val username = prefs.getString("${credentialRef}_username", null)
        val password = prefs.getString("${credentialRef}_password", null)
        if (username == null || password == null) return null
        return WebDavCredentials(username, password)
    }

    fun delete(credentialRef: String) {
        prefs.edit {
            remove("${credentialRef}_username")
                .remove("${credentialRef}_password")
        }
    }
}