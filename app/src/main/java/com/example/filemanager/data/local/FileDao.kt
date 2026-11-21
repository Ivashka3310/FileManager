package com.example.filemanager.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface FileDao {
    @Query("SELECT * FROM files ORDER BY isDirectory DESC, name ASC")
    fun getFilesPagingSource(): PagingSource<Int, FileEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(files: List<FileEntity>)

    @Query("DELETE from files")
    suspend fun clearAll()
}