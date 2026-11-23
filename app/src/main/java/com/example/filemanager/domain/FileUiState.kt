package com.example.filemanager.domain

sealed interface FileUiState {
    data object Loading : FileUiState
    data class Success(val content: String) : FileUiState
    data class Error(val message: String) : FileUiState
}