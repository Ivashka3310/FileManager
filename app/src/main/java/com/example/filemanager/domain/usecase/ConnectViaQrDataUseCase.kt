package com.example.filemanager.domain.usecase

import com.example.filemanager.domain.FileRepository

class ConnectViaQrDataUseCase(private val repository: FileRepository) {
    suspend operator fun invoke(qrData: String) = repository.connectViaQrData(qrData)
}