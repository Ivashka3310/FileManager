package com.example.shared.requests

data class DeleteFilesRequest(
    val relativePaths: List<String>,
    val recursive: Boolean = true
)