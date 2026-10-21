package com.example.filemanager.presentation.ui.screen.ManageScreen

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilePresent
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.filemanager.presentation.ui.viewmodel.ManageViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageScreen(
    viewModel: ManageViewModel = viewModel()
) {
    val messages by viewModel.uiState.collectAsState()
    val files by viewModel.files.collectAsState()
    var showQRScanner by remember { mutableStateOf(false) }
    var showConnectionLoad by remember { mutableStateOf(false) }

    val context = LocalContext.current

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        LaunchedEffect(messages) {
            if (messages.isNotEmpty()) {
                Toast.makeText(context, messages.last(), Toast.LENGTH_SHORT).show()
            }
        }

        if (messages.isEmpty()) {
            Column(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = "Подключение не обнаружено", style = MaterialTheme.typography.titleMedium)
                Text(text = "Для соединения с файловой системой удалённого устройства, подключитесь к той же сети и отсканируйте предоставленный QR код")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = { showQRScanner = true }
                    ) {
                        Text("Сканировать QR код")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 8.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                items(files.sortedBy { file -> !file.isDirectory }) { file ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (file.isDirectory) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = "Folder",
                                    modifier = Modifier
                                        .scale(1.5f)
                                        .padding(horizontal = 8.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.FilePresent,
                                    contentDescription = "File",
                                    modifier = Modifier
                                        .scale(1.5f)
                                        .padding(horizontal = 8.dp)
                                )
                            }

                            Column(
                                modifier = Modifier.fillMaxWidth()
                                    .padding(4.dp)
                            ) {
                                Text(text = file.name, style = MaterialTheme.typography.titleMedium)
                                Text(text = file.path)
                            }
                        }
                    }
                }
            }
        }

        if (showQRScanner) {
            QRScannerField(
                onResult = { qrData ->
                    try {
                        viewModel.getFiles(qrData)
                        showConnectionLoad = true
                    } catch (e: Exception) {
                        Toast.makeText(context, "Connection error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                    showQRScanner = false
                },
                onBack = {
                    showQRScanner = false
                }
            )
        }
    }
}