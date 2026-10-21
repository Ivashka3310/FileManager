package com.example.filemanager.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow

interface ConnectionManager {
    val messageFlow: SharedFlow<String>
    suspend fun connectViaQrData(qrData: String)
    suspend fun requestFiles(path: String)
}