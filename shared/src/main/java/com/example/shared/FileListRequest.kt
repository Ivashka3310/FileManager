package com.example.shared

data class FileListRequest(
    val path: String,
    val offset: Int,
    val limit: Int
)
