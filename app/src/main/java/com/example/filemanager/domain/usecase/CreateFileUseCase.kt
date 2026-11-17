package com.example.filemanager.domain.usecase

import com.example.filemanager.domain.FileRepository

class CreateFileUseCase(private val repository: FileRepository) {
    suspend operator fun invoke(path: String, isDirectory: Boolean) = repository.createFile(
        path = path,
        isDirectory = isDirectory
    )
}