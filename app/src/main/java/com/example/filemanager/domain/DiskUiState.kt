package com.example.filemanager.domain

import com.example.shared.models.DiskInfo

sealed interface DisksUiState {
    data object Loading : DisksUiState
    data class Success(val disks: List<DiskInfo>) : DisksUiState
    data class Error(val message: String) : DisksUiState
}