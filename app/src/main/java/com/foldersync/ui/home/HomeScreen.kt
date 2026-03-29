package com.foldersync.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SyncDisabled
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.foldersync.data.db.entity.SyncRunEntity
import com.foldersync.domain.model.SyncStatus
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToEditor: (profileId: Long?) -> Unit,
    onNavigateToHistory: (profileId: Long) -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var profileToDelete by remember { mutableStateOf<ProfileWithStatus?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Delete confirmation dialog
    profileToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { profileToDelete = null },
            title = { Text("Delete Profile") },
            text = {
                Text("Delete \"${item.profile.name}\"? This will stop scheduled syncs but won't delete any files.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.onDeleteProfile(item.profile.id)
                        profileToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { profileToDelete = null }) {
                    Text("Cancel")
                }
            },
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("FolderSync") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onNavigateToEditor(null) }) {
                Icon(Icons.Default.Add, contentDescription = "Add profile")
            }
        },
    ) { padding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            state.profiles.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.SyncDisabled,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No sync profiles yet",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Tap + to create your first one",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(
                        items = state.profiles,
                        key = { it.profile.id },
                    ) { item ->
                        ProfileCard(
                            item = item,
                            onToggle = { viewModel.onToggleProfile(item.profile) },
                            onSyncNow = {
                                viewModel.onSyncNow(item.profile.id)
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        "Sync triggered for ${item.profile.name}"
                                    )
                                }
                            },
                            onEdit = { onNavigateToEditor(item.profile.id) },
                            onHistory = { onNavigateToHistory(item.profile.id) },
                            onDelete = { profileToDelete = item },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileCard(
    item: ProfileWithStatus,
    onToggle: () -> Unit,
    onSyncNow: () -> Unit,
    onEdit: () -> Unit,
    onHistory: () -> Unit,
    onDelete: () -> Unit,
) {
    val profile = item.profile
    val lastRun = item.lastRun

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Top row: name + switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = profile.name,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = profile.remoteUrl,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
                Switch(
                    checked = profile.enabled,
                    onCheckedChange = { onToggle() },
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Status row
            Row(verticalAlignment = Alignment.CenterVertically) {
                val status = lastRun?.status ?: SyncStatus.NEVER
                StatusDot(status)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formatStatus(status, lastRun),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                IconButton(onClick = onSyncNow) {
                    Icon(Icons.Default.Sync, contentDescription = "Sync now")
                }
                IconButton(onClick = onHistory) {
                    Icon(Icons.Default.History, contentDescription = "History")
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
fun StatusDot(status: SyncStatus) {
    val color by animateColorAsState(
        targetValue = when (status) {
            SyncStatus.NEVER -> MaterialTheme.colorScheme.outlineVariant
            SyncStatus.RUNNING -> MaterialTheme.colorScheme.tertiary
            SyncStatus.SUCCESS -> MaterialTheme.colorScheme.primary
            SyncStatus.PARTIAL -> MaterialTheme.colorScheme.secondary
            SyncStatus.FAILED -> MaterialTheme.colorScheme.error
            SyncStatus.CANCELLED -> MaterialTheme.colorScheme.outlineVariant
        },
        label = "statusDot",
    )
    Box(
        modifier = Modifier
            .size(10.dp)
            .background(color, CircleShape),
    )
}

private fun formatStatus(status: SyncStatus, lastRun: SyncRunEntity?): String {
    if (lastRun == null) return "Never synced"
    val timeAgo = formatTimeAgo(lastRun.finishedAt ?: lastRun.startedAt)
    return when (status) {
        SyncStatus.NEVER -> "Never synced"
        SyncStatus.RUNNING -> "Syncing..."
        SyncStatus.SUCCESS -> "Synced $timeAgo \u00b7 ${lastRun.filesUploaded} up, ${lastRun.filesDownloaded} down"
        SyncStatus.PARTIAL -> "Partial sync $timeAgo"
        SyncStatus.FAILED -> "Failed $timeAgo"
        SyncStatus.CANCELLED -> "Cancelled $timeAgo"
    }
}

private fun formatTimeAgo(epochMillis: Long): String {
    val diff = System.currentTimeMillis() - epochMillis
    val minutes = diff / 60_000
    val hours = minutes / 60
    val days = hours / 24
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        else -> "${days}d ago"
    }
}