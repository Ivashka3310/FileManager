package com.example.shared.models

@net.jcip.annotations.Immutable
data class FileItem(
    val name: String,
    val path: String,
    val size: Long,
    val isDirectory: Boolean,
    val createdAt: String,
    val lastModified: String
)
