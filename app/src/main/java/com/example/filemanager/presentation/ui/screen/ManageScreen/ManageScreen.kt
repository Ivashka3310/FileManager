package com.example.filemanager.presentation.ui.screen.ManageScreen

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.filemanager.presentation.ui.viewmodel.ManageViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun ManageScreen(
    viewModel: ManageViewModel = viewModel()
) {
    val messages by viewModel.uiState.collectAsState()
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
                    .fillMaxSize(),
                ) {
                items(messages) { msg ->
                    Text(msg)
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