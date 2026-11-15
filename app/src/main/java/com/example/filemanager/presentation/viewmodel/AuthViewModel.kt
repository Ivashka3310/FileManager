package com.example.filemanager.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.filemanager.domain.usecase.ConnectViaQrDataUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AuthViewModel(
    private val connectViaQrDataUseCase: ConnectViaQrDataUseCase,
) : ViewModel() {
    fun connect(qrData: String) {
        viewModelScope.launch(Dispatchers.IO) {
            connectViaQrDataUseCase(qrData)
        }
    }
}