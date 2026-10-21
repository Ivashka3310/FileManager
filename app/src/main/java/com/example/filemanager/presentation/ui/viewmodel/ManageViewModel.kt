package com.example.filemanager.presentation.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.filemanager.domain.ConnectionManager
import com.example.shared.models.FileItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ManageViewModel(
    private val connectionManager: ConnectionManager
) : ViewModel() {
    val files = mutableListOf<FileItem>()
    private val _uiState = MutableStateFlow<List<String>>(emptyList())
    val uiState = _uiState.asStateFlow()


    init {
        viewModelScope.launch {
            connectionManager.messageFlow.collect { message ->
                _uiState.update { currentList ->
                    currentList + message
                }
            }
        }
    }

    fun getFiles(qrData: String) {
        viewModelScope.launch {
            connectionManager.connectViaQrData(qrData)
            connectionManager.requestFiles(".")
        }
    }
}