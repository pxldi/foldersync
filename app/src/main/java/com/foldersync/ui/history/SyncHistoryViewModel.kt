package com.foldersync.ui.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foldersync.data.db.entity.SyncRunEntity
import com.foldersync.domain.repository.SyncRunRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryUiState(
    val runs: List<SyncRunEntity> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class SyncHistoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val runRepo: SyncRunRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        val profileId = savedStateHandle.get<String>("profileId")?.toLongOrNull()
        if (profileId != null) {
            viewModelScope.launch {
                runRepo.observeByProfile(profileId).collect { runs ->
                    _uiState.update { it.copy(runs = runs, isLoading = false) }
                }
            }
        }
    }
}