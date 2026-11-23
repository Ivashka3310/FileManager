package com.example.filemanager.presentation.ui.component.dialogs

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.filemanager.R

@Composable
fun DeleteDialog(
    count: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Удалить?") },
        text = {
            Text(
                text = pluralStringResource(
                    id = R.plurals.delete_confirmation_message,
                    count = count,
                    count
                )
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
    DeleteDialog(2, onDismiss =  {}, onConfirm = {})
}