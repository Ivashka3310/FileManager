package com.example.filemanager.domain

import androidx.paging.PagingData
import com.example.shared.models.DiskInfo
import com.example.shared.models.FileItem
import kotlinx.coroutines.flow.Flow

interface FileRepository {
    suspend fun connectViaQrData(qrData: String): Result<Unit>
    suspend fun getLocalDisks(): List<DiskInfo>
    suspend fun getFiles(path: String, offset: Int, limit: Int, filter: String? = null): List<FileItem>
    suspend fun createFile(path: String, isDirectory: Boolean): Boolean
    suspend fun copyFile(sourcePath: String, destinationPath: String, overwrite: Boolean): Boolean
    suspend fun renameFile(oldPath: String, newPath: String): Boolean
    suspend fun deleteFile(path: String, recursive: Boolean): Boolean
    fun getPagerFiles(path: String, filter: String? = null): Flow<PagingData<FileItem>>
    suspend fun getJsonFileContent(path: String): String
    suspend fun getXmlFileContent(path: String): String
    suspend fun createZip(
        archivePath: String,
        archiveName: String,
        relativePaths: List<String>
    )
}