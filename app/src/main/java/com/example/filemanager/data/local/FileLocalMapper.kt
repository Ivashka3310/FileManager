package com.example.filemanager.data.local

import com.example.shared.models.FileItem

class FileLocalMapper {
    fun toDomain(fileEntity: FileEntity): FileItem {
        return FileItem(
            path = fileEntity.path,
            name = fileEntity.name,
            isDirectory = fileEntity.isDirectory,
            size = fileEntity.size,
            createdAt = fileEntity.createdAt,
            lastModified = fileEntity.lastModified
        )
    }

    fun toEntity(fileItem: FileItem): FileEntity {
        return FileEntity(
            path = fileItem.path,
            name = fileItem.name,
            isDirectory = fileItem.isDirectory,
            size = fileItem.size,
            createdAt = fileItem.createdAt,
            lastModified = fileItem.lastModified
        )
    }
}