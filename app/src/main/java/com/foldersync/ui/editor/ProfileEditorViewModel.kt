package com.foldersync.ui.editor

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foldersync.data.db.entity.ConnectionEntity
import com.foldersync.data.db.entity.SyncProfileEntity
import com.foldersync.data.prefs.CredentialStore
import com.foldersync.data.worker.SyncScheduler
import com.foldersync.domain.model.ConflictStrategy
import com.foldersync.domain.model.ConnectionTestResult
import com.foldersync.domain.model.SyncDirection
import com.foldersync.domain.model.WebDavCredentials
import com.foldersync.domain.repository.ConnectionRepository
import com.foldersync.domain.repository.SyncProfileRepository
import com.foldersync.domain.repository.WebDavRepository
import com.foldersync.domain.usecase.TestConnectionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditorUiState(
    val name: String = "",
    val remoteUrl: String = "",
    val username: String = "",
    val password: String = "",
    val localUri: Uri? = null,
    val direction: SyncDirection = SyncDirection.UPLOAD,
    val conflictStrategy: ConflictStrategy = ConflictStrategy.LOCAL_WINS,
    val intervalMinutes: Int = 60,
    val wifiOnly: Boolean = true,
    val chargingOnly: Boolean = false,
    val isEditing: Boolean = false,
    val isTesting: Boolean = false,
    val testResult: String? = null,
    val isSaving: Boolean = false,
    val savedProfileId: Long? = null,
    val scheduledHour: Int? = null,
    val scheduledMinute: Int = 0,
    val connections: List<ConnectionEntity> = emptyList(),
    val selectedConnectionId: Long? = null,
    val remotePath: String = "",  // subfolder within the connection's base URL
)

@HiltViewModel
class ProfileEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val profileRepo: SyncProfileRepository,
    private val connectionRepo: ConnectionRepository,
    private val credentialStore: CredentialStore,
    private val testConnection: TestConnectionUseCase,
    private val syncScheduler: SyncScheduler,
    val webDavRepository: WebDavRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private var existingProfileId: Long? = null

    init {
        // Load available connections
        viewModelScope.launch {
            connectionRepo.observeAll().collect { connections ->
                _uiState.update { it.copy(connections = connections) }
            }
        }

        // Load existing profile if editing
        val profileIdArg = savedStateHandle.get<String>("profileId")
        if (profileIdArg != null && profileIdArg != "new") {
            existingProfileId = profileIdArg.toLongOrNull()
            existingProfileId?.let { loadProfile(it) }
        }
    }

    private fun loadProfile(id: Long) {
        viewModelScope.launch {
            val profile = profileRepo.getById(id) ?: return@launch
            val creds = credentialStore.get(profile.credentialRef)

            _uiState.update {
                it.copy(
                    name = profile.name,
                    remoteUrl = profile.remoteUrl,
                    username = creds?.username ?: "",
                    password = creds?.password ?: "",
                    localUri = Uri.parse(profile.localUri),
                    direction = profile.direction,
                    conflictStrategy = profile.conflictStrategy,
                    intervalMinutes = profile.intervalMinutes,
                    scheduledHour = profile.scheduledHour,
                    scheduledMinute = profile.scheduledMinute,
                    wifiOnly = profile.wifiOnly,
                    chargingOnly = profile.chargingOnly,
                    isEditing = true,
                )
            }
        }
    }

    fun updateName(value: String) { _uiState.update { it.copy(name = value) } }
    fun updateRemoteUrl(value: String) { _uiState.update { it.copy(remoteUrl = value) } }
    fun updateUsername(value: String) { _uiState.update { it.copy(username = value) } }
    fun updatePassword(value: String) { _uiState.update { it.copy(password = value) } }
    fun updateLocalUri(value: Uri) { _uiState.update { it.copy(localUri = value) } }
    fun updateDirection(value: SyncDirection) { _uiState.update { it.copy(direction = value) } }
    fun updateConflictStrategy(value: ConflictStrategy) { _uiState.update { it.copy(conflictStrategy = value) } }
    fun updateIntervalMinutes(value: Int) { _uiState.update { it.copy(intervalMinutes = value) } }
    fun updateScheduledTime(hour: Int?, minute: Int) { _uiState.update { it.copy(scheduledHour = hour, scheduledMinute = minute) } }
    fun updateWifiOnly(value: Boolean) { _uiState.update { it.copy(wifiOnly = value) } }
    fun updateChargingOnly(value: Boolean) { _uiState.update { it.copy(chargingOnly = value) } }
    fun updateSelectedConnection(id: Long?) { _uiState.update { it.copy(selectedConnectionId = id) } }
    fun updateRemotePath(value: String) { _uiState.update { it.copy(remotePath = value) } }

    fun getCredentials(credRef: String): WebDavCredentials? = credentialStore.get(credRef)

    fun testConnection() {
        viewModelScope.launch {
            _uiState.update { it.copy(isTesting = true, testResult = null) }
            val state = _uiState.value
            val result = testConnection(
                state.remoteUrl,
                WebDavCredentials(state.username, state.password),
            )
            _uiState.update {
                it.copy(
                    isTesting = false,
                    testResult = when (result) {
                        is ConnectionTestResult.Success -> "Connection successful!"
                        is ConnectionTestResult.Failure -> "Failed: ${result.message}"
                    },
                )
            }
        }
    }

    fun testConnectionFromSelected() {
        viewModelScope.launch {
            _uiState.update { it.copy(isTesting = true, testResult = null) }
            val state = _uiState.value
            val connection = state.selectedConnectionId?.let { connectionRepo.getById(it) }
            val creds = connection?.let { credentialStore.get(it.credentialRef) }

            if (connection == null || creds == null) {
                _uiState.update { it.copy(isTesting = false, testResult = "No connection selected") }
                return@launch
            }

            val url = if (state.remotePath.isNotBlank()) {
                "${connection.baseUrl.trimEnd('/')}/${state.remotePath.trim('/')}/"
            } else {
                connection.baseUrl
            }

            val result = testConnection(url, creds)
            _uiState.update {
                it.copy(
                    isTesting = false,
                    testResult = when (result) {
                        is ConnectionTestResult.Success -> "Connection successful!"
                        is ConnectionTestResult.Failure -> "Failed: ${result.message}"
                    },
                )
            }
        }
    }

    fun saveProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val state = _uiState.value

            // Resolve connection
            val connection = state.selectedConnectionId?.let { connectionRepo.getById(it) }

            val remoteUrl = if (connection != null && state.remotePath.isNotBlank()) {
                "${connection.baseUrl.trimEnd('/')}/${state.remotePath.trim('/')}/"
            } else if (connection != null) {
                connection.baseUrl
            } else {
                state.remoteUrl  // fallback to manual URL
            }

            val credRef = connection?.credentialRef
                ?: "profile_${existingProfileId ?: System.currentTimeMillis()}"

            // Save credentials if no connection (manual mode)
            if (connection == null) {
                credentialStore.save(credRef, WebDavCredentials(state.username, state.password))
            }

            val profile = SyncProfileEntity(
                id = existingProfileId ?: 0,
                name = state.name.ifBlank { "My Sync" },
                localUri = state.localUri.toString(),
                remoteUrl = remoteUrl,
                credentialRef = credRef,
                direction = state.direction,
                conflictStrategy = state.conflictStrategy,
                intervalMinutes = state.intervalMinutes,
                wifiOnly = state.wifiOnly,
                chargingOnly = state.chargingOnly,
                enabled = true,
                connectionId = state.selectedConnectionId,
                scheduledHour = state.scheduledHour,
                scheduledMinute = state.scheduledMinute,
            )
            val id = profileRepo.insert(profile)
            val saved = profileRepo.getById(id)!!
            syncScheduler.schedule(saved)

            _uiState.update { it.copy(isSaving = false, savedProfileId = id) }
        }
    }
}