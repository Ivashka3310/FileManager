package com.example.filemanager.data

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.example.filemanager.data.local.FileEntity
import com.example.filemanager.data.local.FileLocalDataSource
import com.example.filemanager.data.local.FileLocalMapper
import com.example.filemanager.data.remote.FileRemoteDataSource
import com.example.filemanager.domain.SessionStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

@OptIn(ExperimentalPagingApi::class)
class FileRemoteMediator(
    private val sessionStorage: SessionStorage,
    private val currentPath: String,
    private val fileRemoteDataSource: FileRemoteDataSource,
    private val localDataSource: FileLocalDataSource,
    private val localMapper: FileLocalMapper,
    private val filter: String? = null
) : RemoteMediator<Int, FileEntity>() {

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, FileEntity>
    ): MediatorResult {
        return try {
            val url = sessionStorage.url.first()
            val token = sessionStorage.accessToken.first()

            if (url.isNullOrEmpty() || token.isNullOrEmpty()) {
                sessionStorage.clearSession()
                error("Session expired")
            }

            val offset = when (loadType) {
                LoadType.REFRESH -> 0
                LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
                LoadType.APPEND -> { state.pages.sumOf { it.data.size } }
            }

            val files = fileRemoteDataSource.getFiles(
                url = url,
                token = token,
                filter = filter,
                path = currentPath,
                offset = offset,
                limit = state.config.pageSize,
            )

            if (loadType == LoadType.REFRESH) {
                localDataSource.clear()
                localDataSource.insertFiles(files.map { localMapper.toEntity(it) })
            } else {
                localDataSource.insertFiles(files.map { localMapper.toEntity(it) })
            }

            MediatorResult.Success(endOfPaginationReached = files.isEmpty())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            MediatorResult.Error(e)
        }
    }
}
