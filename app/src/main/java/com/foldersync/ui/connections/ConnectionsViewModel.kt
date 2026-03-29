package com.foldersync.ui.connections

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foldersync.data.db.entity.ConnectionEntity
import com.foldersync.data.prefs.CredentialStore
import com.foldersync.domain.model.ConnectionTestResult
import com.foldersync.domain.model.WebDavCredentials
import com.foldersync.domain.repository.ConnectionRepository
import com.foldersync.domain.usecase.TestConnectionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConnectionsUiState(
    val connections: List<ConnectionEntity> = emptyList(),
    val isLoading: Boolean = true,
    // Add/Edit dialog state
    val showDialog: Boolean = false,
    val editingConnection: ConnectionEntity? = null,
    val dialogName: String = "",
    val dialogBaseUrl: String = "",
    val dialogUsername: String = "",
    val dialogPassword: String = "",
    val dialogTesting: Boolean = false,
    val dialogTestResult: String? = null,
)

@HiltViewModel
class ConnectionsViewModel @Inject constructor(
    private val connectionRepo: ConnectionRepository,
    private val credentialStore: CredentialStore,
    private val testConnection: TestConnectionUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConnectionsUiState())
    val uiState: StateFlow<ConnectionsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            connectionRepo.observeAll().collect { connections ->
                _uiState.update { it.copy(connections = connections, isLoading = false) }
            }
        }
    }

    fun showAddDialog() {
        _uiState.update {
            it.copy(
                showDialog = true,
                editingConnection = null,
                dialogName = "",
                dialogBaseUrl = "",
                dialogUsername = "",
                dialogPassword = "",
                dialogTestResult = null,
            )
        }
    }

    fun showEditDialog(connection: ConnectionEntity) {
        val creds = credentialStore.get(connection.credentialRef)
        _uiState.update {
            it.copy(
                showDialog = true,
                editingConnection = connection,
                dialogName = connection.name,
                dialogBaseUrl = connection.baseUrl,
                dialogUsername = creds?.username ?: "",
                dialogPassword = creds?.password ?: "",
                dialogTestResult = null,
            )
        }
    }

    fun dismissDialog() {
        _uiState.update { it.copy(showDialog = false) }
    }

    fun updateDialogName(value: String) { _uiState.update { it.copy(dialogName = value) } }
    fun updateDialogBaseUrl(value: String) { _uiState.update { it.copy(dialogBaseUrl = value) } }
    fun updateDialogUsername(value: String) { _uiState.update { it.copy(dialogUsername = value) } }
    fun updateDialogPassword(value: String) { _uiState.update { it.copy(dialogPassword = value) } }

    fun testDialogConnection() {
        viewModelScope.launch {
            _uiState.update { it.copy(dialogTesting = true, dialogTestResult = null) }
            val state = _uiState.value
            val result = testConnection(
                state.dialogBaseUrl,
                WebDavCredentials(state.dialogUsername, state.dialogPassword),
            )
            _uiState.update {
                it.copy(
                    dialogTesting = false,
                    dialogTestResult = when (result) {
                        is ConnectionTestResult.Success -> "Connected!"
                        is ConnectionTestResult.Failure -> "Failed: ${result.message}"
                    },
                )
            }
        }
    }

    fun saveConnection() {
        viewModelScope.launch {
            val state = _uiState.value
            val existing = state.editingConnection
            val credRef = existing?.credentialRef ?: "conn_${System.currentTimeMillis()}"

            credentialStore.save(credRef, WebDavCredentials(state.dialogUsername, state.dialogPassword))

            if (existing != null) {
                connectionRepo.update(
                    existing.copy(
                        name = state.dialogName,
                        baseUrl = state.dialogBaseUrl,
                        credentialRef = credRef,
                    )
                )
            } else {
                connectionRepo.insert(
                    ConnectionEntity(
                        name = state.dialogName.ifBlank { "My Server" },
                        baseUrl = state.dialogBaseUrl,
                        credentialRef = credRef,
                    )
                )
            }

            _uiState.update { it.copy(showDialog = false) }
        }
    }

    fun deleteConnection(id: Long) {
        viewModelScope.launch { connectionRepo.delete(id) }
    }
}