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
    suspend fun getJsonContent(relativePath: String, maxFileSize: Long = 5 * 1024 * 1024): String
    suspend fun getXmlContent(relativePath: String, maxFileSize: Long = 5 * 1024 * 1024): String
    suspend fun createZip(
        relativePaths: List<String>,
        targetZipRelativePath: String,
        maxSingleFileSize: Long = 50 * 1024 * 1024,
        maxTotalSize: Long = 200 * 1024 * 1024,
        maxFiles: Int = 1000
    )
    suspend fun extractZip(
        zipPath: String,
        relativePath: String,
        maxEntries: Int = 1000,
        maxTotalSize: Int = 200 * 1024 * 1024,
        maxCompressionRatio: Double = 20.0,
        bufferSize: Int = 8192
    )
}