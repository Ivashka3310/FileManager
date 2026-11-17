package com.example.filemanager.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.filemanager.domain.DisksUiState
import com.example.filemanager.domain.usecase.GetFilesPagedUseCase
import com.example.filemanager.domain.usecase.GetLocalDisksUseCase
import com.example.shared.models.FileItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch


class ManageViewModel(
    private val getFilesPagedUseCase: GetFilesPagedUseCase,
    private val getLocalDisksUseCase: GetLocalDisksUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow<List<String>>(emptyList())
    val uiState = _uiState.asStateFlow()

    private val _currentPath = MutableStateFlow<String?>(null)
    val currentPath = _currentPath.asStateFlow()

    private val _currentFilter = MutableStateFlow<String?>(null)
    val currentFilter = _currentFilter.asStateFlow()

    private val _disks = MutableStateFlow<DisksUiState>(DisksUiState.Loading)
    val disks = _disks.asStateFlow()

    init {
        loadDisks()
    }

    fun loadDisks() {
        viewModelScope.launch {
            _disks.value = DisksUiState.Loading
            getLocalDisksUseCase().fold(
                onSuccess = { _disks.value = DisksUiState.Success(it) },
                onFailure = { _disks.value = DisksUiState.Error(it.message ?: "Ошибка загрузки дисков") }
            )
        }
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    val filesFlow: Flow<PagingData<FileItem>> = combine(
        _currentPath,
        _currentFilter
    ) { path, filter ->
        path to filter
    }.flatMapLatest { (path, filter) ->
        if (path == null) {
            flowOf(PagingData.empty())
        } else {
            getFilesPagedUseCase(path, filter)
        }
    }.cachedIn(viewModelScope)

    fun openDir(newPath: String) {
        _currentPath.value = newPath
    }
    fun goBack() {
        val path = currentPath.value ?: return

        val normalizedPath = path.trimEnd('/')
        val parentPath = normalizedPath.substringBeforeLast('/', missingDelimiterValue = "")

        if (parentPath.isEmpty()) {
            _currentPath.value = null
        } else {
            _currentPath.value = parentPath
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