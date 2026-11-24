package com.example.filemanager.domain.usecase

import com.example.filemanager.domain.interfaces.FileRepository

class GetJsonFileContentUseCase(private val repository: FileRepository) {
    suspend operator fun invoke(path: String) = repository.getJsonFileContent(path)
}