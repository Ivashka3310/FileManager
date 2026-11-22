package com.example.filemanager.presentation.ui.component.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun RenameDialog(
    oldName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(oldName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Переименовать") },
        text = {
            Column {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Новое имя") },
                    isError = (('/' in name) || (".." in name)),
                    supportingText = {
                        if (name.isBlank()) Text("Поле не может быть пустым")
                        else if ('/' in name) Text("Недопустимый символ \"/\"")
                        else if (".." in name) Text("В названии не может быть двух точек подряд")
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled =  ((name.isNotBlank()) && ('/' !in name) && (".." !in name))
            ) {
                Text("Переименовать")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}


@Preview(showSystemUi = true, showBackground = true)
@Composable
fun RenameDialogPreview() {
    RenameDialog("File.txt", {}, {_ -> })
}