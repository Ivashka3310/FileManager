package com.example.filemanager.domain

import com.example.shared.models.FileItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow

interface ConnectionManager {
    val messageFlow: SharedFlow<ServerResponse>
    suspend fun connectViaQrData(qrData: String)
    suspend fun requestFiles(path: String)
}