package com.example.filemanager.data

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.example.filemanager.data.local.FileEntity
import com.example.filemanager.data.local.FileLocalDataSource
import com.example.filemanager.data.local.FileLocalMapper
import com.example.filemanager.data.remote.FileRemoteDataSource
import com.example.filemanager.domain.FileRepository
import com.example.filemanager.domain.SessionStorage
import com.example.shared.models.DiskInfo
import com.example.shared.models.FileItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.net.URI

class FileRepositoryImpl(
    private val fileRemoteDataSource: FileRemoteDataSource,
    private val localDataSource: FileLocalDataSource,
    private val sessionStorage: SessionStorage,
    private val mapper: FileLocalMapper
) : FileRepository {
    override suspend fun connectViaQrData(qrData: String): Result<Unit> = runCatching {
        val uri = URI(qrData)
        val host = uri.host ?: error("Invalid host in QR: $qrData")
        val secret = uri.query
            ?.split('&')
            ?.mapNotNull { it.split('=', limit = 2).takeIf { p -> p.size == 2 } }
            ?.firstOrNull { it[0] == "secret" }
            ?.get(1)
            ?: error("Secret not found in QR: $qrData")

        val port = if (uri.port == -1) 80 else uri.port
        val url = "http://$host:$port"
        sessionStorage.saveUrl(url)

        val token = fileRemoteDataSource.login(url, secret)

        sessionStorage.saveToken(token)
    }

    override suspend fun getLocalDisks(): List<DiskInfo> {
        val (url, token) = getSession()
        return fileRemoteDataSource.getLocalDisks(url, token)
    }

    override suspend fun getFiles(
        path: String,
        offset: Int,
        limit: Int,
        filter: String?
    ): List<FileItem> {
        val (url, token) = getSession()
        return fileRemoteDataSource.getFiles(
            url = url,
            token = token,
            path = path,
            offset = offset,
            limit = limit,
            filter = filter
        )
    }

    override suspend fun createFile(path: String, isDirectory: Boolean): Boolean {
        val (url, token) = getSession()
        return fileRemoteDataSource.createFile(url, token, path, isDirectory)
    }

    override suspend fun copyFile(sourcePath: String, destinationPath: String, overwrite: Boolean): Boolean {
        val (url, token) = getSession()
        return fileRemoteDataSource.copyFile(
            url = url,
            token = token,
            sourcePath = sourcePath,
            destinationPath = destinationPath,
            overwrite = overwrite
        )
    }

    override suspend fun renameFile(oldPath: String, newPath: String): Boolean {
        val (url, token) = getSession()
        return fileRemoteDataSource.renameFile(
            url = url,
            token = token,
            oldPath = oldPath,
            newPath = newPath
        )
    }

    override suspend fun deleteFile(path: String, recursive: Boolean): Boolean {
        val (url, token) = getSession()
        return fileRemoteDataSource.deleteFile(
            url = url,
            token = token,
            path = path,
            recursive = recursive
        )
    }

    @OptIn(ExperimentalPagingApi::class)
    override fun getPagerFiles(path: String, filter: String?): Flow<PagingData<FileItem>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                initialLoadSize = 20,
                prefetchDistance = 5,
                enablePlaceholders = false
            ),
            remoteMediator = FileRemoteMediator(
                sessionStorage = sessionStorage,
                currentPath = path,
                fileRemoteDataSource = fileRemoteDataSource,
                localDataSource = localDataSource,
                localMapper = mapper,
                filter = filter,
            ),
            pagingSourceFactory = {
                localDataSource.getFilesPagingSource()
            }
        ).flow.map { pagingData ->
            pagingData.map { fileEntity -> mapper.toDomain(fileEntity) }
        }
    }

    override suspend fun getJsonFileContent(path: String): String {
        val (url, token) = getSession()
        return fileRemoteDataSource.getJsonFileContent(url, token, path)
    }
    private suspend fun getSession(): Pair<String, String> {
        val url = sessionStorage.url.first()
        val token = sessionStorage.accessToken.first()
        if (url.isNullOrEmpty() || token.isNullOrEmpty()) error("Session expired")
        return url to token
    }
}