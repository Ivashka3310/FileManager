package com.example.filemanager.data.local

import androidx.paging.PagingSource
import androidx.room.Transaction

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

    @Transaction
    suspend fun clearAndInsert(files: List<FileEntity>) {
        dao.clearAll()
        dao.insertAll(files)
    }
}