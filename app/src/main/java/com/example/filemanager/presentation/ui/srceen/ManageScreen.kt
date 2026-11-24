package com.example.filemanager.presentation.ui.srceen

import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material3.BottomAppBar
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
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.filemanager.presentation.ui.component.dialogs.CreateDialog
import com.example.filemanager.presentation.ui.component.dialogs.DeleteDialog
import com.example.filemanager.presentation.ui.component.DisksContent
import com.example.filemanager.presentation.ui.component.FileSelectionTopAppBar
import com.example.filemanager.presentation.ui.component.FilesContent
import com.example.filemanager.presentation.ui.component.dialogs.CreateZipDialog
import com.example.filemanager.presentation.ui.component.dialogs.InfoDialog
import com.example.filemanager.presentation.ui.component.dialogs.RenameDialog
import com.example.filemanager.presentation.viewmodel.ManageViewModel
import com.example.shared.models.FileItem
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ManageScreen(
    viewModel: ManageViewModel = viewModel(),
    openJson: (String) -> Unit,
    openXml: (String) -> Unit
) {
    val messages by viewModel.uiState.collectAsState()
    val currentPath by viewModel.currentPath.collectAsState()
    val disksState by viewModel.disks.collectAsState()
    val selectedFiles by viewModel.selectedFiles.collectAsState()
    val isSelectionMode = selectedFiles.isNotEmpty()
    val isSingleFile = selectedFiles.size == 1

    val lazyPagingItems: LazyPagingItems<FileItem>? = if (currentPath != null) {
        viewModel.filesFlow.collectAsLazyPagingItems()
    } else null

    val context = LocalContext.current

    var showCreateDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<FileItem?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showCreateZipDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = true) {
        if (isSelectionMode) {
            viewModel.clearSelection()
        } else {
            viewModel.goBack()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (isSelectionMode) {
                val file = selectedFiles.first()
                FileSelectionTopAppBar(
                    selectedCount = selectedFiles.size,
                    isSingleFile = isSingleFile,
                    isCompressed = file.name.endsWith(".zip"),
                    onClose = { viewModel.clearSelection() },
                    onZip = { showCreateZipDialog = true },
                    onExtract = {
                        viewModel.extractZip(file.path, file.name)
                        viewModel.clearSelection()
                    },
                    onRename = { renameTarget = file },
                    onDelete = { showDeleteDialog = true },
                    onInfo = { showInfoDialog = true }
                )
            } else {
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
            }
        },
        floatingActionButton = {
            if (!isSelectionMode && currentPath != null) {
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

        if (showCreateZipDialog) {
            CreateZipDialog(
                onDismiss = { showCreateZipDialog = false },
                onConfirm = { archiveName ->
                    viewModel.createZip(archiveName)
                    viewModel.clearSelection()
                    showCreateZipDialog = false
                }
            ) 
        }

        if (showDeleteDialog) {
            DeleteDialog(
                count = selectedFiles.size,
                onDismiss = { showDeleteDialog = false },
                onConfirm = {
                    viewModel.deleteSelectedFiles()
                    viewModel.clearSelection()
                    showDeleteDialog = false
                }
            )
        }

        if (showInfoDialog) {
            InfoDialog(
                file = selectedFiles.first(),
                onDismiss = { showInfoDialog = false }
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
                onFileClick = { file ->
                    if (isSelectionMode) {
                        viewModel.toggleSelection(file)
                    } else {
                        if (file.isDirectory) {
                            viewModel.openDir(file.path)
                        } else if (file.path.endsWith(".json")) {
                            openJson(file.path)
                        } else if (file.path.endsWith(".xml")) {
                            openXml(file.path)
                        }
                    }
                },
                onFileLongClick = { file ->
                    viewModel.toggleSelection(file)
                },
                isSelectionMode = isSelectionMode,
                selectedFiles = selectedFiles
            )
        }

        renameTarget?.let { file ->
            RenameDialog(
                oldName = file.name,
                onDismiss = { renameTarget = null },
                onConfirm = { newName ->
                    if (file.name != newName) {
                        viewModel.renameFile(file.path, newName)
                    }
                    viewModel.clearSelection()
                    renameTarget = null
                }
            )
        }
    }
}