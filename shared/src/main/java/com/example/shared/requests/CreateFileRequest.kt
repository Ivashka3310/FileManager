package com.example.shared.requests

data class CreateFileRequest(
    val path: String,
    val isDirectory: Boolean = false
)
