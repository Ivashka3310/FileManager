package com.example.filemanager.data.remote

import com.example.shared.models.DiskInfo
import com.example.shared.models.FileItem

interface FileRemoteDataSource {
    suspend fun login(url: String, secret: String): String
    suspend fun getLocalDisks(url: String, token: String): List<DiskInfo>

    suspend fun getFiles(
        url: String,
        token: String,
        path: String,
        offset: Int,
        limit: Int,
        filter: String? = null
    ): List<FileItem>

    suspend fun createFile(
        url: String,
        token: String,
        path: String,
        isDirectory: Boolean
    ): Boolean

    suspend fun copyFile(
        url: String,
        token: String,
        sourcePath: String,
        destinationPath: String,
        overwrite: Boolean
    ): Boolean

    suspend fun renameFile(
        url: String,
        token: String,
        oldPath: String,
        newPath: String
    ): Boolean

    suspend fun deleteFiles(
        url: String,
        token: String,
        relativePaths: List<String>,
        recursive: Boolean
    ): Boolean

    suspend fun getJsonFileContent(
        url: String,
        token: String,
        path: String
    ): String

    suspend fun getXmlFileContent(
        url: String,
        token: String,
        path: String
    ): String

    suspend fun createZip(
        url: String,
        token: String,
        archivePath: String,
        archiveName: String,
        relativePaths: List<String>
    )

    suspend fun extractZip(
        url: String,
        token: String,
        zipPath: String,
        targetDir: String
    )
}