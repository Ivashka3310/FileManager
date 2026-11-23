package com.example.filemanager.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "files")
data class FileEntity(
    @PrimaryKey val path: String,
    val name: String,
    val isDirectory: Boolean,
    val size: Long,
    val createdAt: String,
    val lastModified: String
)