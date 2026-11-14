package com.example.filemanager.presentation.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.filemanager.data.FilesPagingSource
import com.example.filemanager.domain.ConnectionManager
import com.example.filemanager.domain.ServerResponse
import com.example.filemanager.domain.usecase.ConnectViaQrDataUseCase
import com.example.filemanager.domain.usecase.GetFilesPagedUseCase
import com.example.shared.models.FileItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


class ManageViewModel(
    private val getFilesPagedUseCase: GetFilesPagedUseCase
) : ViewModel() {
//    private val _files = MutableStateFlow<List<FileItem>>(emptyList())
//    val files = _files.asStateFlow()
    private val _uiState = MutableStateFlow<List<String>>(emptyList())
    val uiState = _uiState.asStateFlow()

    private val _currentPath = MutableStateFlow(".")
    val currentPath = _currentPath.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val filesFlow: Flow<PagingData<FileItem>> = _currentPath
        .flatMapLatest { path ->
            getFilesPagedUseCase(path)
        }
        .cachedIn(viewModelScope)

    fun openDir(newPath: String) {
        _currentPath.value = newPath
    }
//    fun connectAndGetFiles(qrData: String) {
//        viewModelScope.launch {
//            try {
//                connectionManager.connectViaQrData(qrData)
//                connectionManager.requestFiles(".")
//            } catch (e: Exception) {
//                _uiState.update { currentList ->
//                    currentList + (e.message ?: "Unknown error occurred during connection")
//                }
//            }
//        }
//    }
//
//    fun onCardClick(file: FileItem) {
//        viewModelScope.launch {
//            if (file.isDirectory) {
//                currentPath.value = file.path
//                connectionManager.requestFiles(file.path)
//            } else {
//                //
//            }
//        }
//    }
//
    fun goBack() {
        val path = currentPath.value
            .trimEnd('/')
            .substringBeforeLast('/')
        if (path.isEmpty() || path == "/") {
            val message = "Нажмите ещё раз, чтобы выйти из приложения"
            _uiState.update { currentList ->
                currentList + message
            }
            return
        } else {
            _currentPath.value = path
        }
    }
}