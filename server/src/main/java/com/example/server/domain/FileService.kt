package com.example.server.domain

import com.example.shared.requests.FileListRequest
import com.example.shared.models.DiskInfo
import com.example.shared.models.FileItem

interface FileService {
    suspend fun getLocalDisks(): List<DiskInfo>
    suspend fun getFiles(fileListRequest: FileListRequest): List<FileItem>
    suspend fun createFile(relativePath: String, isDirectory: Boolean): Boolean
    suspend fun copyFile(sourcePath: String, destinationPath: String, overwrite: Boolean): Boolean
    suspend fun renameFile(oldPath: String, newPath: String): Boolean
    suspend fun deleteFile(relativePath: String, recursive: Boolean): Boolean
    suspend fun getJsonContent(relativePath: String): String
    suspend fun getXmlContent(relativePath: String): String
}