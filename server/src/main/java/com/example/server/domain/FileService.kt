package com.example.server.domain

import com.example.shared.models.FileItem

interface FileService {
    suspend fun getFiles(relativePath: String, offset: Int, limit: Int): List<FileItem>
    suspend fun deleteFile(relativePath: String): Boolean
}