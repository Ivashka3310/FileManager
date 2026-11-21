package com.example.filemanager.domain

sealed interface JsonUiState {
    data object Loading : JsonUiState
    data class Success(val content: String) : JsonUiState
    data class Error(val message: String) : JsonUiState
}