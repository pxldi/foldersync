package com.foldersync.ui.editor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.foldersync.domain.model.ConflictStrategy
import com.foldersync.domain.model.SyncDirection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditorScreen(
    onNavigateBack: () -> Unit,
    viewModel: ProfileEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Navigate back after save
    LaunchedEffect(state.savedProfileId) {
        if (state.savedProfileId != null) {
            onNavigateBack()
        }
    }

    // SAF folder picker
    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri: Uri? ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
            viewModel.updateLocalUri(uri)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditing) "Edit Profile" else "New Profile") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = viewModel::saveProfile,
                        enabled = !state.isSaving
                                && (state.selectedConnectionId != null || state.remoteUrl.isNotBlank())
                                && (state.selectedConnectionId != null || state.username.isNotBlank())
                                && state.localUri != null,
                        modifier = Modifier.padding(end = 8.dp),
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Text("Save")
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // --- Connection Picker ---
            if (state.connections.isNotEmpty()) {
                DropdownSelector(
                    label = "Connection",
                    selected = state.connections.find { it.id == state.selectedConnectionId }?.name
                        ?: "Manual (enter URL below)",
                    options = listOf("Manual (enter URL below)") + state.connections.map { it.name },
                    onSelect = { selected ->
                        val conn = state.connections.find { it.name == selected }
                        viewModel.updateSelectedConnection(conn?.id)
                    },
                )
            }

            // --- Profile Name ---
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::updateName,
                label = { Text("Profile Name") },
                placeholder = { Text("My Nextcloud Sync") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            if (state.selectedConnectionId == null) {

                // --- WebDAV URL ---
                OutlinedTextField(
                    value = state.remoteUrl,
                    onValueChange = viewModel::updateRemoteUrl,
                    label = { Text("WebDAV URL") },
                    placeholder = { Text("https://cloud.example.com/remote.php/dav/files/user/") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                // --- Username ---
                OutlinedTextField(
                    value = state.username,
                    onValueChange = viewModel::updateUsername,
                    label = { Text("Username") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                // --- Password ---
                var passwordVisible by remember { mutableStateOf(false) }

                OutlinedTextField(
                    value = state.password,
                    onValueChange = viewModel::updatePassword,
                    label = { Text("App Password") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (passwordVisible)
                        VisualTransformation.None
                    else
                        PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible)
                                    Icons.Default.VisibilityOff
                                else
                                    Icons.Default.Visibility,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            )
                        }
                    },
                )
            }

            // Show remote folder browser if connection IS selected
            if (state.selectedConnectionId != null) {
                val connection = state.connections.find { it.id == state.selectedConnectionId }
                var showBrowser by remember { mutableStateOf(false) }

                OutlinedTextField(
                    value = state.remotePath,
                    onValueChange = viewModel::updateRemotePath,
                    label = { Text("Remote Subfolder (optional)") },
                    placeholder = { Text("e.g. Photos/Backup") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                OutlinedButton(
                    onClick = { showBrowser = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Browse Remote Folders")
                }

                if (showBrowser && connection != null) {
                    val creds = viewModel.getCredentials(connection.credentialRef)
                    if (creds != null) {
                        RemoteFolderBrowserDialog(
                            baseUrl = connection.baseUrl,
                            credentials = creds,
                            webDavRepository = viewModel.webDavRepository,
                            onSelect = { path ->
                                viewModel.updateRemotePath(path)
                                showBrowser = false
                            },
                            onDismiss = { showBrowser = false },
                        )
                    }
                }
            }

            // --- Test Connection ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        if (state.selectedConnectionId != null) {
                            viewModel.testConnectionFromSelected()
                        } else {
                            viewModel.testConnection()
                        }
                    },
                    enabled = !state.isTesting && (
                            state.selectedConnectionId != null
                                    || (state.remoteUrl.isNotBlank() && state.username.isNotBlank())
                            ),
                ) {
                    if (state.isTesting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                    Text("Test Connection")
                }
                state.testResult?.let { result ->
                    Text(
                        text = result,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (result.startsWith("Connection"))
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error,
                    )
                }
            }

            // --- Folder Picker ---
            Button(
                onClick = { folderPicker.launch(null) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.localUri != null) "Change Folder" else "Pick Local Folder")
            }
            state.localUri?.let {
                Text(
                    text = "Folder: ${it.lastPathSegment}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // --- Sync Direction ---
            DropdownSelector(
                label = "Sync Direction",
                selected = state.direction.displayName(),
                options = SyncDirection.entries.map { it.displayName() },
                onSelect = { selected ->
                    val dir = SyncDirection.entries.first { it.displayName() == selected }
                    viewModel.updateDirection(dir)
                },
            )

            // --- Conflict Strategy ---
            DropdownSelector(
                label = "Conflict Strategy",
                selected = state.conflictStrategy.displayName(),
                options = ConflictStrategy.entries.map { it.displayName() },
                onSelect = { selected ->
                    val strat = ConflictStrategy.entries.first { it.displayName() == selected }
                    viewModel.updateConflictStrategy(strat)
                },
            )

            // --- Interval ---
            val intervals = listOf(15, 30, 60, 120, 360, 720, 1440)
            DropdownSelector(
                label = "Sync Interval",
                selected = formatInterval(state.intervalMinutes),
                options = intervals.map { formatInterval(it) },
                onSelect = { selected ->
                    val mins = intervals.first { formatInterval(it) == selected }
                    viewModel.updateIntervalMinutes(mins)
                },
            )

            // --- Or: daily at specific time ---
            var showTimePicker by remember { mutableStateOf(false) }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Sync at specific time daily")
                Switch(
                    checked = state.scheduledHour != null,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            viewModel.updateScheduledTime(3, 0) // default 3:00 AM
                        } else {
                            viewModel.updateScheduledTime(null, 0)
                        }
                    },
                )
            }

            if (state.scheduledHour != null) {
                OutlinedButton(
                    onClick = { showTimePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Sync at: %02d:%02d".format(state.scheduledHour, state.scheduledMinute))
                }
            }

            if (showTimePicker) {
                TimePickerDialog(
                    initialHour = state.scheduledHour ?: 3,
                    initialMinute = state.scheduledMinute,
                    onConfirm = { hour, minute ->
                        viewModel.updateScheduledTime(hour, minute)
                        showTimePicker = false
                    },
                    onDismiss = { showTimePicker = false },
                )
            }

            // --- Constraints ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Wi-Fi only")
                Switch(checked = state.wifiOnly, onCheckedChange = viewModel::updateWifiOnly)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Only while charging")
                Switch(checked = state.chargingOnly, onCheckedChange = viewModel::updateChargingOnly)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownSelector(
    label: String,
    selected: String,
    options: List<String>,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select sync time") },
        text = { TimePicker(state = timePickerState) },
        confirmButton = {
            Button(onClick = { onConfirm(timePickerState.hour, timePickerState.minute) }) {
                Text("OK")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

private fun SyncDirection.displayName(): String = when (this) {
    SyncDirection.UPLOAD -> "Upload only"
    SyncDirection.DOWNLOAD -> "Download only"
    SyncDirection.BIDIRECTIONAL -> "Two-way sync"
}

private fun ConflictStrategy.displayName(): String = when (this) {
    ConflictStrategy.LOCAL_WINS -> "Local wins"
    ConflictStrategy.REMOTE_WINS -> "Remote wins"
    ConflictStrategy.KEEP_BOTH -> "Keep both"
    ConflictStrategy.NEWEST_WINS -> "Newest wins"
}

private fun formatInterval(minutes: Int): String = when (minutes) {
    15 -> "Every 15 minutes"
    30 -> "Every 30 minutes"
    60 -> "Every hour"
    120 -> "Every 2 hours"
    360 -> "Every 6 hours"
    720 -> "Every 12 hours"
    1440 -> "Once a day"
    else -> "Every $minutes minutes"
}