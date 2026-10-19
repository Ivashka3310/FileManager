package com.example.shared.models

import java.nio.file.attribute.FileTime

data class FileItem(
    val name: String,
    val path: String,
    val size: Long,
    val isDirectory: Boolean,
    val createdAt: String,
    val lastModified: String
)

