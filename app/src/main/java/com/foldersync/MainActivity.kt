package com.foldersync

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.foldersync.data.db.entity.SyncProfileEntity
import com.foldersync.domain.model.ConflictStrategy
import com.foldersync.domain.model.SyncDirection
import com.foldersync.domain.model.WebDavCredentials
import com.foldersync.domain.repository.SyncProfileRepository
import com.foldersync.domain.usecase.SyncFolderUseCase
import com.foldersync.domain.usecase.TestConnectionUseCase
import com.foldersync.ui.theme.FolderSyncTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var testConnection: TestConnectionUseCase
    @Inject lateinit var syncFolder: SyncFolderUseCase
    @Inject lateinit var syncProfileRepo: SyncProfileRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FolderSyncTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    var webDavUrl by remember { mutableStateOf("") }
                    var username by remember { mutableStateOf("") }
                    var password by remember { mutableStateOf("") }
                    var selectedFolderUri by remember { mutableStateOf<Uri?>(null) }
                    var statusText by remember { mutableStateOf("") }
                    var isLoading by remember { mutableStateOf(false) }
                    val scope = rememberCoroutineScope()

                    // SAF folder picker launcher
                    // This is the Compose equivalent of
                    // startActivityForResult(Intent(ACTION_OPEN_DOCUMENT_TREE))
                    val folderPicker = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.OpenDocumentTree(),
                    ) { uri: Uri? ->
                        if (uri != null) {
                            // Persist permission so it survives app restart
                            contentResolver.takePersistableUriPermission(
                                uri,
                                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                        android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                            )
                            selectedFolderUri = uri
                            statusText = "Folder selected: ${uri.lastPathSegment}"
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Spacer(modifier = Modifier.height(40.dp))

                        Text(
                            text = "FolderSync — Sync Test",
                            style = MaterialTheme.typography.headlineSmall,
                        )

                        // --- WebDAV URL ---
                        OutlinedTextField(
                            value = webDavUrl,
                            onValueChange = { webDavUrl = it },
                            label = { Text("WebDAV URL") },
                            placeholder = { Text("https://cloud.example.com/remote.php/dav/files/user/") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )

                        // --- Username ---
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("Username") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )

                        // --- Password ---
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("App Password") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )

                        // --- Folder Picker ---
                        Button(
                            onClick = { folderPicker.launch(null) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                if (selectedFolderUri != null) "Change Folder"
                                else "Pick Local Folder"
                            )
                        }

                        if (selectedFolderUri != null) {
                            Text(
                                text = "Selected: ${selectedFolderUri?.lastPathSegment}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }

                        // --- Sync Button ---
                        Button(
                            onClick = {
                                val uri = selectedFolderUri ?: return@Button
                                isLoading = true
                                statusText = "Syncing..."
                                scope.launch {
                                    // 1. Create or update the profile from form data
                                    val profile = SyncProfileEntity(
                                        name = "Test Sync",
                                        localUri = uri.toString(),
                                        remoteUrl = webDavUrl,
                                        credentialRef = "test",
                                        direction = SyncDirection.UPLOAD,
                                        conflictStrategy = ConflictStrategy.LOCAL_WINS,
                                    )
                                    val profileId = syncProfileRepo.insert(profile)

                                    // 2. Now sync using the real profile ID
                                    val result = syncFolder.execute(
                                        profileId = profileId,
                                        localUri = uri,
                                        remoteUrl = webDavUrl,
                                        credentials = WebDavCredentials(username, password),
                                        direction = SyncDirection.UPLOAD,
                                        conflictStrategy = ConflictStrategy.LOCAL_WINS,
                                    )

                                    statusText = buildString {
                                        if (result.success) append("Sync complete!\n")
                                        else append("Sync finished with errors\n")
                                        append("Uploaded: ${result.filesUploaded}\n")
                                        append("Downloaded: ${result.filesDownloaded}\n")
                                        append("Skipped: ${result.filesSkipped}\n")
                                        append("Failed: ${result.filesFailed}\n")
                                        append("Bytes: ${result.bytesTransferred}\n")
                                        if (result.errors.isNotEmpty()) {
                                            append("\nErrors:\n")
                                            result.errors.forEach { append("• $it\n") }
                                        }
                                    }
                                    isLoading = false
                                }
                            },
                            enabled = !isLoading && selectedFolderUri != null
                                    && webDavUrl.isNotBlank() && username.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Sync Now (Upload)")
                        }

                        if (isLoading) {
                            CircularProgressIndicator()
                        }

                        if (statusText.isNotEmpty()) {
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        }
    }
}