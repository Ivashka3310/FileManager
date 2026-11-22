package com.example.filemanager.domain.usecase

import com.example.filemanager.domain.FileRepository

class GetXmlFileContentUseCase(private val repository: FileRepository) {
    suspend operator fun invoke(path: String) = repository.getXmlFileContent(path)
}