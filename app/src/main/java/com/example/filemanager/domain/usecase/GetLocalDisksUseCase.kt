package com.example.filemanager.domain.usecase

import com.example.filemanager.domain.interfaces.FileRepository
import com.example.shared.models.DiskInfo

class GetLocalDisksUseCase(private val repository: FileRepository) {
    suspend operator fun invoke(): Result<List<DiskInfo>> = runCatching {
        repository.getLocalDisks()
    }
}