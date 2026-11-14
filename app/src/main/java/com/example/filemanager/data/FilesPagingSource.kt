package com.example.filemanager.data

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.filemanager.domain.FileRepository
import com.example.shared.models.FileItem
import kotlin.math.max

class FilesPagingSource(
    private val fileRepository: FileRepository,
    private val currentPath: String
) : PagingSource<Int, FileItem>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, FileItem> {
        return try {
            val currentOffset = params.key ?: 0
            val pageSize = params.loadSize

            val files = fileRepository.getFiles(
                path = currentPath,
                offset = currentOffset,
                limit = pageSize
            )

            LoadResult.Page(
                data = files,
                prevKey = if (currentOffset == 0) null else max(0, currentOffset - pageSize),
                nextKey = if (files.size < pageSize) null else currentOffset + files.size
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, FileItem>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(state.config.pageSize)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(state.config.pageSize)
        }
    }
}