package com.example.filemanager.domain.usecase

import com.example.filemanager.domain.FileRepository

class DeleteFilesUseCase(private val repository: FileRepository) {
    suspend operator fun invoke(relativePaths: List<String>, recursive: Boolean) = repository.deleteFiles(
        relativePaths = relativePaths,
        recursive = recursive
    )
}