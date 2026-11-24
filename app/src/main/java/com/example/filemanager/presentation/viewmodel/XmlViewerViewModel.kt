package com.example.filemanager.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.filemanager.domain.interfaces.FileUiState
import com.example.filemanager.domain.usecase.GetXmlFileContentUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class XmlViewerViewModel(
    private val getXmlFileContentUseCase: GetXmlFileContentUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow<FileUiState>(FileUiState.Loading)
    val uiState = _uiState.asStateFlow()

    fun loadXml(path: String) {
        val relativePath = toRelative(path)
        viewModelScope.launch {
            _uiState.value = FileUiState.Loading
            try {
                val content = getXmlFileContentUseCase(relativePath)
                _uiState.value = FileUiState.Success(content)
            } catch (e: Exception) {
                _uiState.value = FileUiState.Error(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    fun onParseError(throwable: Throwable) {
        _uiState.value = FileUiState.Error(throwable.message ?: "Неизвестная ошибка парсинга")
    }

    private fun toRelative(path: String): String {
        val stripped = path.trimStart('/').trimEnd('/')
        return stripped.ifEmpty { "." }
    }
}