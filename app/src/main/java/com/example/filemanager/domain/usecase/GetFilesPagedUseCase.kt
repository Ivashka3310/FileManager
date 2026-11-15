package com.example.filemanager.domain.usecase

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.example.filemanager.data.FilesPagingSource
import com.example.filemanager.domain.FileRepository
import com.example.shared.models.FileItem
import kotlinx.coroutines.flow.Flow

class GetFilesPagedUseCase(private val fileRepository: FileRepository) {
    operator fun invoke(currentPath: String, filter: String? = null): Flow<PagingData<FileItem>> = Pager(
        config = PagingConfig(
            pageSize = 20,
            initialLoadSize = 20,
            prefetchDistance = 5,
            enablePlaceholders = false
        ),
        pagingSourceFactory = {
            FilesPagingSource(
                fileRepository = fileRepository,
                currentPath = currentPath,
                filter = filter
            )
        }
    ).flow
}