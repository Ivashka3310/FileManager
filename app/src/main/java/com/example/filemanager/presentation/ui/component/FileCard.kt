package com.example.filemanager.presentation.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilePresent
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.example.shared.models.FileItem

@Composable
fun FileCard(
    file: FileItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        onClick = onClick
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