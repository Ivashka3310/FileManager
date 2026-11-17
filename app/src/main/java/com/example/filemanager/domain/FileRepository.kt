package com.example.filemanager.domain

import com.example.shared.models.DiskInfo
import com.example.shared.models.FileItem
import kotlinx.coroutines.flow.Flow

interface FileRepository {
    suspend fun connectViaQrData(qrData: String): Result<Unit>
    suspend fun getFiles(path: String, offset: Int, limit: Int, filter: String? = null): List<FileItem>
    suspend fun removeFile(relativePath: String): Flow<String>
    suspend fun getLocalDisks(): List<DiskInfo>
}