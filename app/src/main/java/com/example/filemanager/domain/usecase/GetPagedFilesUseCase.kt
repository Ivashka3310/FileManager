package com.example.filemanager.domain.usecase

import androidx.paging.PagingData
import com.example.filemanager.domain.interfaces.FileRepository
import com.example.shared.models.FileItem
import kotlinx.coroutines.flow.Flow

class GetPagedFilesUseCase(private val fileRepository: FileRepository) {
    operator fun invoke(
        currentPath: String,
        filter: String? = null
    ): Flow<PagingData<FileItem>> {
        return fileRepository.getPagerFiles(
            path = currentPath,
            filter = filter
        )
    }
}