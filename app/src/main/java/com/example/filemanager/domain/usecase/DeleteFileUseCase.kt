package com.example.filemanager.domain.usecase

import com.example.filemanager.domain.FileRepository

class DeleteFileUseCase(private val repository: FileRepository) {
    suspend operator fun invoke(path: String, recursive: Boolean) = repository.deleteFile(
        path = path,
        recursive = recursive
    )
}