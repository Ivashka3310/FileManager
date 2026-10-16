package com.example.server.data

import com.example.shared.models.FileItem
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.fileSize
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile


class FileService(private val rootPath: Path) {
    fun ls(relativePath: String): List<FileItem> {
        val resolvedPath = rootPath.resolve(relativePath).normalize()

        if (!resolvedPath.startsWith(rootPath)) {
            throw SecurityException("Access denied: path is outside root directory")
        }

        return Files.list(resolvedPath).use { stream ->
            stream.map { path ->
                FileItem(
                    name = path.fileName.toString(),
                    path = rootPath.relativize(path).toString(),
                    size = if (path.isRegularFile()) path.fileSize() else 0L,
                    isDirectory = path.isDirectory(),
                    createdAt = Files.getAttribute(path, "creationTime").toString(),
                    lastModified = path.getLastModifiedTime()
                )
            }.toList()
        }
    }
}