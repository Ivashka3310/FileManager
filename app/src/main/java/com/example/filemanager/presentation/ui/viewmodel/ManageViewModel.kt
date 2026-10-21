package com.example.filemanager.presentation.ui.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.filemanager.domain.ConnectionManager
import com.example.filemanager.domain.ServerResponse
import com.example.shared.models.FileItem
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ManageViewModel(
    private val connectionManager: ConnectionManager
) : ViewModel() {
    private val _files = MutableStateFlow<List<FileItem>>(emptyList())
    val files = _files.asStateFlow()
    private val _uiState = MutableStateFlow<List<String>>(emptyList())
    val uiState = _uiState.asStateFlow()

    val currentPath = mutableStateOf("/")

    init {
        viewModelScope.launch {
            connectionManager.messageFlow.collect { response ->
                when (response) {
                    is ServerResponse.FileList -> {
                        _files.update { response.files }
                    }
                    is ServerResponse.Error,
                    is ServerResponse.Status -> {
                        val message = when (response) {
                            is ServerResponse.Error -> response.message
                            is ServerResponse.Status -> response.message
                            else -> ""
                        }
                        _uiState.update { currentList ->
                            currentList + message
                        }
                    }
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