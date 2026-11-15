package com.example.filemanager.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.filemanager.domain.usecase.GetFilesPagedUseCase
import com.example.shared.models.FileItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update


class ManageViewModel(
    private val getFilesPagedUseCase: GetFilesPagedUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow<List<String>>(emptyList())
    val uiState = _uiState.asStateFlow()

    private val _currentPath = MutableStateFlow(".")
    val currentPath = _currentPath.asStateFlow()

    private val _currentFilter = MutableStateFlow<String?>(null)
    val currentFilter = _currentFilter.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val filesFlow: Flow<PagingData<FileItem>> = combine(
        _currentPath,
        _currentFilter
    ) { path, filter ->
        path to filter
    }.flatMapLatest { (path, filter) ->
            getFilesPagedUseCase(path, filter)
        }
        .cachedIn(viewModelScope)

    fun openDir(newPath: String) {
        _currentPath.value = newPath
    }
    fun goBack() {
        val path = currentPath.value
            .trimEnd('/')
            .substringBeforeLast('/')
        if (path.isEmpty() || path == "/") {
            val message = "Нажмите ещё раз, чтобы выйти из приложения"
            _uiState.update { currentList ->
                currentList + message
            }
            return
        } else {
            _currentPath.value = path
        }
    }

    fun setFilter(filter: String) {
        if (currentFilter.value.isNullOrEmpty()) {
            _currentFilter.value = "hidden=true"
        } else {
            _currentFilter.value = null
        }
    }
}