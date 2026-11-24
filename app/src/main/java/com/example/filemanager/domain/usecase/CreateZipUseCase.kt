package com.example.filemanager.domain.usecase

import com.example.filemanager.domain.interfaces.FileRepository

class CreateZipUseCase(private val repository: FileRepository) {
    suspend operator fun invoke(
        archivePath: String,
        archiveName: String,
        relativePaths: List<String>
    ) = repository.createZip(archivePath, archiveName, relativePaths)
}