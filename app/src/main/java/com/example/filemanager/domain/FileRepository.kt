package com.example.filemanager.domain

import com.example.shared.models.DiskInfo
import com.example.shared.models.FileItem

interface FileRemoteDataSource {
    suspend fun connectViaQrData(qrData: String): Result<Unit>
    suspend fun getLocalDisks(): List<DiskInfo>
    suspend fun getFiles(path: String, offset: Int, limit: Int, filter: String? = null): List<FileItem>
    suspend fun createFile(path: String, isDirectory: Boolean): Boolean
    suspend fun copyFile(sourcePath: String, destinationPath: String, overwrite: Boolean): Boolean
    suspend fun renameFile(oldPath: String, newPath: String): Boolean
    suspend fun deleteFile(path: String, recursive: Boolean): Boolean
}