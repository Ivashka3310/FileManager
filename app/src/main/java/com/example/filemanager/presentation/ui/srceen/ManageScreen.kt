package com.example.filemanager.presentation.ui.srceen

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.filemanager.presentation.ui.component.dialogs.CreateDialog
import com.example.filemanager.presentation.ui.component.dialogs.DeleteDialog
import com.example.filemanager.presentation.ui.component.DisksContent
import com.example.filemanager.presentation.ui.component.FilesContent
import com.example.filemanager.presentation.ui.component.dialogs.RenameDialog
import com.example.filemanager.presentation.viewmodel.ManageViewModel
import com.example.shared.models.FileItem
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageScreen(
    viewModel: ManageViewModel = viewModel()
) {
    val messages by viewModel.uiState.collectAsState()
    val currentPath by viewModel.currentPath.collectAsState()
    val disksState by viewModel.disks.collectAsState()

    val lazyPagingItems: LazyPagingItems<FileItem>? = if (currentPath != null) {
        viewModel.filesFlow.collectAsLazyPagingItems()
    } else null

    val context = LocalContext.current

    var showCreateDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<FileItem?>(null) }
    var menuTarget by remember { mutableStateOf<FileItem?>(null) }
    var deleteTarget by remember { mutableStateOf<FileItem?>(null) }

    BackHandler(enabled = true) { viewModel.goBack() }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(currentPath ?: "Диски") },
                actions = {
                    if (currentPath != null) {
                        IconButton(onClick = { viewModel.setFilter("") }) {
                            Icon(Icons.Default.FilterAlt, "Filter")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (currentPath != null) {
                FloatingActionButton(onClick = { showCreateDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Создать")
                }
            }
        }
    ) { innerPadding ->
        LaunchedEffect(messages) {
            if (messages.isNotEmpty()) {
                Toast.makeText(context, messages.last(), Toast.LENGTH_SHORT).show()
                viewModel.clearMessages()
            }
        }

        if (showCreateDialog) {
            CreateDialog(
                onDismiss = { showCreateDialog = false },
                onConfirm = { name, isDir ->
                    viewModel.createFile(name, isDir)
                    showCreateDialog = false
                }
            )
        }

        if (currentPath == null) {
            DisksContent(
                state = disksState,
                innerPadding = innerPadding,
                onRetry = viewModel::loadDisks,
                onDiskClick = viewModel::openDisk
            )
        } else if (lazyPagingItems != null) {
            FilesContent(
                items = lazyPagingItems,
                innerPadding = innerPadding,
                menuTarget = menuTarget,
                onMenuTargetChange = { menuTarget = it },
                onFileClick = { file ->
                    if (file.isDirectory) viewModel.openDir(file.path)
                },
                onFileLongClick = { menuTarget = it },
                onRenameClick = { file ->
                    renameTarget = file
                    menuTarget = null
                },
                onDeleteClick = { file ->
                    deleteTarget = file
                    menuTarget = null
                }
            )
        }

        renameTarget?.let { file ->
            RenameDialog(
                oldName = file.name,
                onDismiss = { renameTarget = null },
                onConfirm = { newName ->
                    viewModel.renameFile(file.path, newName)
                    renameTarget = null
                }
            )
        }

        deleteTarget?.let { file ->
            DeleteDialog(
                file = file,
                onDismiss = { deleteTarget = null },
                onConfirm = {
                    viewModel.deleteFile(file.path)
                    deleteTarget = null
                }
            )
        }
    }
}