package com.example.shared.requests

data class RenameFileRequest(
    val oldPath: String,
    val newPath: String
)