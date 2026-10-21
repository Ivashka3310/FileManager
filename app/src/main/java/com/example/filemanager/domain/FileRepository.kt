package com.example.filemanager.domain

import com.example.shared.models.FileItem
import kotlinx.coroutines.flow.Flow

interface FileRepository {
    suspend fun getFiles(relativePath: String): Flow<List<FileItem>>
    suspend fun removeFile(relativePath: String): Flow<String>
}