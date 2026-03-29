package com.foldersync.ui.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.foldersync.domain.model.RemoteFile
import com.foldersync.domain.model.WebDavCredentials
import com.foldersync.domain.repository.WebDavRepository
import kotlinx.coroutines.launch

@Composable
fun RemoteFolderBrowserDialog(
    baseUrl: String,
    credentials: WebDavCredentials,
    webDavRepository: WebDavRepository,
    onSelect: (String) -> Unit,   // relative path from baseUrl
    onDismiss: () -> Unit,
) {
    var currentPath by remember { mutableStateOf("") }
    var folders by remember { mutableStateOf<List<RemoteFile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Load folders when path changes
    LaunchedEffect(currentPath) {
        isLoading = true
        error = null
        try {
            val url = if (currentPath.isEmpty()) baseUrl
            else "${baseUrl.trimEnd('/')}/$currentPath/"
            val files = webDavRepository.listFiles(url, credentials)
            folders = files.filter { it.isDirectory }
        } catch (e: Exception) {
            error = e.message
        }
        isLoading = false
    }

    // New folder dialog
    if (showNewFolderDialog) {
        NewFolderDialog(
            onConfirm = { folderName ->
                showNewFolderDialog = false
                scope.launch {
                    val parentUrl = if (currentPath.isEmpty()) baseUrl
                    else "${baseUrl.trimEnd('/')}/$currentPath/"
                    val newUrl = "${parentUrl.trimEnd('/')}/$folderName/"
                    webDavRepository.createDirectory(newUrl, credentials)
                    // Refresh
                    currentPath = currentPath // triggers LaunchedEffect re-run
                    // Actually need to force refresh:
                    isLoading = true
                    try {
                        val files = webDavRepository.listFiles(parentUrl, credentials)
                        folders = files.filter { it.isDirectory }
                    } catch (_: Exception) {}
                    isLoading = false
                }
            },
            onDismiss = { showNewFolderDialog = false },
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Select Remote Folder")
                Text(
                    text = "/${currentPath.ifEmpty { "(root)" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        text = {
            Column {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                } else if (error != null) {
                    Text("Error: $error", color = MaterialTheme.colorScheme.error)
                } else {
                    LazyColumn(
                        modifier = Modifier.height(300.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        // Go up
                        if (currentPath.isNotEmpty()) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            currentPath = currentPath
                                                .trimEnd('/')
                                                .substringBeforeLast('/', "")
                                        }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        Icons.Default.FolderOpen,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("..", style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                        }

                        items(folders) { folder ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        currentPath = if (currentPath.isEmpty()) folder.path
                                        else "${currentPath.trimEnd('/')}/${folder.name}"
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Default.Folder,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    folder.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSelect(currentPath) }) {
                Text("Select This Folder")
            }
        },
        dismissButton = {
            Row {
                IconButton(onClick = { showNewFolderDialog = true }) {
                    Icon(Icons.Default.CreateNewFolder, contentDescription = "New folder")
                }
                OutlinedButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}

@Composable
fun NewFolderDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Folder") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Folder name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank(),
            ) { Text("Create") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}