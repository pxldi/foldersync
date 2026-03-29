package com.foldersync.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foldersync.data.db.entity.SyncProfileEntity
import com.foldersync.data.db.entity.SyncRunEntity
import com.foldersync.data.worker.SyncScheduler
import com.foldersync.domain.repository.SyncProfileRepository
import com.foldersync.domain.repository.SyncRunRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val profiles: List<ProfileWithStatus> = emptyList(),
    val isLoading: Boolean = true,
)

data class ProfileWithStatus(
    val profile: SyncProfileEntity,
    val lastRun: SyncRunEntity? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val profileRepo: SyncProfileRepository,
    private val runRepo: SyncRunRepository,
    private val syncScheduler: SyncScheduler,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadProfiles()
    }

    private fun loadProfiles() {
        viewModelScope.launch {
            profileRepo.observeAll().collect { profiles ->
                // Re-fetch last run for each profile every time profiles emit
                // This also triggers on initial load
                refreshProfileStatuses(profiles)
            }
        }

        // Also refresh periodically while the screen is visible
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(10_000) // Refresh every 10 seconds
                val profiles = profileRepo.getAll()
                refreshProfileStatuses(profiles)
            }
        }
    }

    private suspend fun refreshProfileStatuses(
        profiles: List<SyncProfileEntity>,
    ) {
        val withStatus = profiles.map { profile ->
            ProfileWithStatus(
                profile = profile,
                lastRun = runRepo.getLatestByProfile(profile.id),
            )
        }
        _uiState.update {
            it.copy(profiles = withStatus, isLoading = false)
        }
    }

    fun onToggleProfile(profile: SyncProfileEntity) {
        viewModelScope.launch {
            val updated = profile.copy(enabled = !profile.enabled)
            profileRepo.update(updated)
            if (updated.enabled) {
                syncScheduler.schedule(updated)
            } else {
                syncScheduler.cancel(updated.id)
            }
        }
    }

    fun onSyncNow(profileId: Long) {
        syncScheduler.syncNow(profileId)
    }

    fun onDeleteProfile(profileId: Long) {
        viewModelScope.launch {
            syncScheduler.cancel(profileId)
            profileRepo.delete(profileId)
        }
    }
}