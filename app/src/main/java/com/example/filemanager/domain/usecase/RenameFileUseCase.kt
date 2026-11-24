package com.example.filemanager.domain.usecase

import com.example.filemanager.domain.interfaces.FileRepository

class RenameFileUseCase(private val repository: FileRepository) {
    suspend operator fun invoke(oldPath: String, newPath: String) = repository.renameFile(
        oldPath = oldPath,
        newPath = newPath
    )
}