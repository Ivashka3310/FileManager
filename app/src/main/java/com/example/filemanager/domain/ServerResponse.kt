package com.example.filemanager.domain

import com.example.shared.models.FileItem

sealed class ServerResponse {
    data class FileList(val files: List<FileItem>) : ServerResponse()
    data class Error(val message: String) : ServerResponse()
    data class Status(val message: String) : ServerResponse()
}
