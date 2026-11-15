package com.example.filemanager.domain

import kotlinx.coroutines.flow.Flow

interface SessionStorage {
    val url: Flow<String?>
    val accessToken: Flow<String?>

    suspend fun saveUrl(newUrl: String)
    suspend fun saveToken(token: String)
    suspend fun clearSession()
}