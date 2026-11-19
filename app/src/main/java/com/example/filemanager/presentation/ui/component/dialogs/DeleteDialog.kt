package com.example.filemanager.presentation.ui.component.dialogs

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.shared.models.FileItem

@Composable
fun DeleteDialog(
    file: FileItem,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Удалить?") },
        text = {
            Text(
                if (file.isDirectory) {
                    "Папка \"${file.name}\" и её содержимое будут удалены безвозвратно."
                } else {
                    "Файл \"${file.name}\" будет удалён безвозвратно."
                }
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Удалить")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}


@Preview(showSystemUi = true, showBackground = true)
@Composable
fun DeleteDialogPreview() {
    val file = FileItem(
        name = "File.example",
        path = "/File.example",
        size = 10,
        isDirectory = false,
        createdAt = "",
        lastModified = ""
    )
    DeleteDialog(file, {}, {})
}