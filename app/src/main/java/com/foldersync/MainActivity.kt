package com.foldersync

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import com.foldersync.domain.model.ConnectionTestResult
import com.foldersync.domain.model.WebDavCredentials
import com.foldersync.domain.usecase.TestConnectionUseCase
import com.foldersync.ui.theme.FolderSyncTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Hilt injects this automatically because TestConnectionUseCase
    // has @Inject constructor and all its dependencies are provided.
    @Inject
    lateinit var testConnection: TestConnectionUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FolderSyncTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    var result by remember { mutableStateOf<String?>(null) }
                    var isLoading by remember { mutableStateOf(false) }
                    val scope = rememberCoroutineScope()

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = "FolderSync",
                            style = MaterialTheme.typography.headlineMedium,
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                isLoading = true
                                result = null
                                scope.launch {
                                    val testResult = testConnection(
                                        url = "https://cloud.pxldi.de/remote.php/dav/files/YOUR_USERNAME/",
                                        credentials = WebDavCredentials(
                                            username = "YOUR_USERNAME",
                                            password = "YOUR_APP_PASSWORD",
                                        ),
                                    )
                                    result = when (testResult) {
                                        is ConnectionTestResult.Success -> "Connection successful!"
                                        is ConnectionTestResult.Failure -> "Failed: ${testResult.message}"
                                    }
                                    isLoading = false
                                }
                            },
                            enabled = !isLoading,
                        ) {
                            Text("Test WebDAV Connection")
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (isLoading) {
                            CircularProgressIndicator()
                        }

                        result?.let { text ->
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = text,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (text.startsWith("Connection"))
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        }
    }
}