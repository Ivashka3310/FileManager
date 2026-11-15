package com.example.server.domain

import com.example.shared.models.FileItem

interface FileService {
    suspend fun getFiles(
        relativePath: String,
        offset: Int,
        limit: Int,
        filter: String? = null,
        extension: String? = null,
        nameQuery: String? = null,
        orderBy: String? = null
    ): List<FileItem>
    suspend fun deleteFile(relativePath: String): Boolean
}