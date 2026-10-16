package com.example.shared

data class FileItem(
    val name: String,
    val path: String,
    val size: Long,
    val isDirectory: Boolean,
    val createdAt: Long,
    val lastModified: Long
)

