package com.example.server.domain

import com.example.shared.models.FileItem

interface FileService {
    suspend fun getFiles(relativePath: String): List<FileItem>
    suspend fun deleteFile(relativePath: String): Boolean
}