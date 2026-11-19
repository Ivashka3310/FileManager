package com.example.filemanager.presentation.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.filemanager.domain.DisksUiState

@Composable
fun DisksContent(
    state: DisksUiState,
    innerPadding: PaddingValues,
    onRetry: () -> Unit,
    onDiskClick: (com.example.shared.models.DiskInfo) -> Unit
) {
    when (state) {
        is DisksUiState.Loading -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }

        is DisksUiState.Error -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(state.message, color = MaterialTheme.colorScheme.error)
                Button(onClick = onRetry) { Text("Повторить") }
            }
        }

        is DisksUiState.Success -> LazyColumn(
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.disks) { disk ->
                DiskCard(disk = disk, onClick = { onDiskClick(disk) })
            }
        }
    }
}