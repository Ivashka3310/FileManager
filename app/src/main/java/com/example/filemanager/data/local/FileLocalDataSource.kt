package com.example.filemanager.data.local

import androidx.paging.PagingSource

class FileLocalDataSource(
    private val dao: FileDao
) {
    fun getFilesPagingSource(): PagingSource<Int, FileEntity> {
        return dao.getFilesPagingSource()
    }
    suspend fun insertFiles(files: List<FileEntity>) {
        dao.insertAll(files)
    }

    suspend fun clear() {
        dao.clearAll()
    }
}