package com.example.server.data

import com.example.server.domain.FileService
import com.example.shared.models.FileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.fileSize
import kotlin.io.path.getAttribute
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile


class FileServiceImpl(private val rootPath: Path): FileService {
    override suspend fun getFiles(relativePath: String): List<FileItem> {
        val resolvedPath = rootPath.resolve(relativePath).normalize()

        if (!resolvedPath.startsWith(rootPath)) {
            throw SecurityException("Access denied: path is outside root directory! Your path is: $resolvedPath")
        }

        return withContext(Dispatchers.IO) {
            Files.list(resolvedPath)
        }.use { stream ->
            stream.map { path ->
                FileItem(
                    name = path.fileName.toString(),
                    path = path.toString(),
                    size = if (path.isRegularFile()) path.fileSize() else 0L,
                    isDirectory = path.isDirectory(),
                    createdAt = path.getAttribute("creationTime")?.toString() ?: "unknown",
                    lastModified = path.getLastModifiedTime().toString()
                )
            }.toList()
        }
    }
}