package com.example.filemanager.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.filemanager.domain.DisksUiState
import com.example.filemanager.domain.usecase.CreateFileUseCase
import com.example.filemanager.domain.usecase.DeleteFileUseCase
import com.example.filemanager.domain.usecase.GetPagedFilesUseCase
import com.example.filemanager.domain.usecase.GetLocalDisksUseCase
import com.example.filemanager.domain.usecase.RenameFileUseCase
import com.example.shared.models.DiskInfo
import com.example.shared.models.FileItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ManageViewModel(
    private val getPagedFilesUseCase: GetPagedFilesUseCase,
    private val getLocalDisksUseCase: GetLocalDisksUseCase,
    private val createFileUseCase: CreateFileUseCase,
    private val renameFileUseCase: RenameFileUseCase,
    private val deleteFileUseCase: DeleteFileUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<List<String>>(emptyList())
    val uiState = _uiState.asStateFlow()

    private val _currentPath = MutableStateFlow<String?>(null)
    val currentPath = _currentPath.asStateFlow()

    private val _currentFilter = MutableStateFlow<String?>(null)
    val currentFilter = _currentFilter.asStateFlow()

    private val _disks = MutableStateFlow<DisksUiState>(DisksUiState.Loading)
    val disks = _disks.asStateFlow()

    private val refreshTrigger = MutableStateFlow(0)

    init {
        loadDisks()
    }

    fun clearMessages() {
        _uiState.update { emptyList() }
    }

    fun loadDisks() {
        viewModelScope.launch {
            if (_disks.value !is DisksUiState.Success) {
                _disks.value = DisksUiState.Loading
            }
            getLocalDisksUseCase().fold(
                onSuccess = { _disks.value = DisksUiState.Success(it) },
                onFailure = { _disks.value = DisksUiState.Error(it.message ?: "Ошибка загрузки дисков") }
            )
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val filesFlow: Flow<PagingData<FileItem>> = combine(
        _currentPath,
        _currentFilter,
        refreshTrigger
    ) { path, filter, _ ->
        path to filter
    }.flatMapLatest { (path, filter) ->
        if (path == null) {
            flowOf(PagingData.empty())
        } else {
            getPagedFilesUseCase(toRelative(path), filter)
        }
    }.cachedIn(viewModelScope)

    fun openDisk(disk: DiskInfo) {
        val relativePath = if (disk.mountPoint == "/") {
            "."
        } else {
            disk.mountPoint.removePrefix("/")
        }
        openDir(relativePath)
    }

    fun openDir(newPath: String) {
        _currentPath.value = toRelative(newPath)
    }

    fun goBack() {
        val path = currentPath.value ?: return
        val normalizedPath = path.trimEnd('/')

        if (normalizedPath.isEmpty() || normalizedPath == ".") {
            _currentPath.value = null
            return
        }

        val parentPath = normalizedPath.substringBeforeLast('/', missingDelimiterValue = "")
        _currentPath.value = parentPath.ifEmpty { "." }
    }

    fun setFilter(filter: String) {
        _currentFilter.value = if (currentFilter.value.isNullOrEmpty()) "hidden=true" else null
    }

    fun createFile(name: String, isDirectory: Boolean) {
        val currentDir = currentPath.value ?: return
        val cleanDir = toRelative(currentDir)

        val newPath = if (cleanDir == "." || cleanDir.isEmpty()) {
            name
        } else {
            "$cleanDir/$name"
        }

        viewModelScope.launch {
            runCatching { createFileUseCase(newPath, isDirectory) }
                .onSuccess {
                    showMessage(if (isDirectory) "Папка создана" else "Файл создан")
                    refreshTrigger.update { it + 1 }
                }
                .onFailure { showMessage(it.message ?: "Ошибка создания") }
        }
    }

    fun renameFile(oldPath: String, newName: String) {
        val relativeOldPath = toRelative(oldPath)
        val parentDir = relativeOldPath.substringBeforeLast('/', missingDelimiterValue = "")

        val newPath = if (parentDir.isEmpty()) {
            newName
        } else {
            "$parentDir/$newName"
        }

        viewModelScope.launch {
            runCatching { renameFileUseCase(relativeOldPath, newPath) }
                .onSuccess {
                    showMessage("Переименовано")
                    refreshTrigger.update { it + 1 }
                }
                .onFailure { showMessage(it.message ?: "Ошибка переименования") }
        }
    }

    fun deleteFile(path: String) {
        val relativePath = toRelative(path)

        viewModelScope.launch {
            runCatching { deleteFileUseCase(relativePath, recursive = true) }
                .onSuccess {
                    showMessage("Удалено")

                    val current = currentPath.value?.let { toRelative(it) }
                    if (current == relativePath) {
                        goBack()
                    }

                    refreshTrigger.update { it + 1 }
                }
                .onFailure { showMessage(it.message ?: "Ошибка удаления") }
        }
    }

    private fun showMessage(msg: String) {
        _uiState.update { it + msg }
    }

    private fun toRelative(path: String): String {
        val stripped = path.trimStart('/').trimEnd('/')
        return stripped.ifEmpty { "." }
    }
}