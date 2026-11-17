package com.example.shared.requests

data class CopyFileRequest(
    val sourcePath: String,
    val destinationPath: String,
    val overwrite: Boolean = false
)