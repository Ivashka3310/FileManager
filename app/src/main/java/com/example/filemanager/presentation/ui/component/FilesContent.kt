package com.example.filemanager.presentation.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import com.example.shared.models.FileItem

@Composable
fun FilesContent(
    items: LazyPagingItems<FileItem>,
    innerPadding: PaddingValues,
    onFileClick: (FileItem) -> Unit,
    onFileLongClick: (FileItem) -> Unit,
    isSelectionMode: Boolean,
    selectedFiles: Set<FileItem>
) {
    val isRefreshing = items.loadState.refresh is LoadState.Loading
    val isFirstLoad = isRefreshing && items.itemCount == 0
    val refreshError = items.loadState.refresh as? LoadState.Error

    val listState = rememberLazyListState()

    PullToRefreshBox(
        isRefreshing = isRefreshing && !isFirstLoad,
        onRefresh = { items.refresh() },
        modifier = Modifier.padding(innerPadding).fillMaxSize()
    ) {
        when {
            isFirstLoad -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            refreshError != null -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Ошибка загрузки: ${refreshError.error.localizedMessage ?: "Неизвестная ошибка"}",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                    Button(onClick = { items.retry() }) { Text("Повторить") }
                }
            }

            else -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    count = items.itemCount,
                    key = items.itemKey { it.path }
                ) { index ->
                    val file = items[index]
                    file?.let {
                        val isSelected = selectedFiles.contains(it)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else Color.Transparent
                                )
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    FileCard(
                                        file = file,
                                        onClick = { onFileClick(it) },
                                        onLongClick = { onFileLongClick(it) },
                                        isSelectionMode = isSelectionMode,
                                        isSelected = isSelected,
                                        onSelectToggle = { onFileClick(it) }
                                    )
                                }
                            }
                        }
                    }
                }

                if (items.loadState.append is LoadState.Loading) {
                    item {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }
}