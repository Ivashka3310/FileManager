package com.example.filemanager.domain.usecase

import com.example.filemanager.domain.FileRepository

class ExtractZipUseCase(private val repository: FileRepository) {
    suspend operator fun invoke(zipPath: String, targetDir: String) = repository.extractZip(zipPath, targetDir)
}