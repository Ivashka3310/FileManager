package com.example.filemanager.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.filemanager.domain.JsonUiState
import com.example.filemanager.domain.usecase.GetJsonFileContentUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class JsonViewerViewModel(
    private val getJsonFileContentUseCase: GetJsonFileContentUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow<JsonUiState>(JsonUiState.Loading)
    val uiState = _uiState.asStateFlow()

    fun loadJson(path: String) {
        val relativePath = toRelative(path)
        viewModelScope.launch {
            _uiState.value = JsonUiState.Loading
            try {
                val content = getJsonFileContentUseCase(relativePath)
                _uiState.value = JsonUiState.Success(content)
            } catch (e: Exception) {
                _uiState.value = JsonUiState.Error(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    fun onParseError(throwable: Throwable) {
        _uiState.value = JsonUiState.Error(throwable.message ?: "Неизвестная ошибка парсинга")
    }

    private fun toRelative(path: String): String {
        val stripped = path.trimStart('/').trimEnd('/')
        return stripped.ifEmpty { "." }
    }
}