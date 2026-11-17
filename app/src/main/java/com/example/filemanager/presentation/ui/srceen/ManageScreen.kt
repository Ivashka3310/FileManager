package com.example.filemanager.presentation.ui.srceen

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.example.filemanager.presentation.ui.component.FileCard
import com.example.filemanager.presentation.viewmodel.ManageViewModel
import com.example.shared.models.FileItem
import com.example.filemanager.domain.DisksUiState
import com.example.filemanager.presentation.ui.component.DiskCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageScreen(
    viewModel: ManageViewModel = viewModel()
) {
    val messages by viewModel.uiState.collectAsState()
    val currentPath by viewModel.currentPath.collectAsState()
    val disksState by viewModel.disks.collectAsState()

    // Paging items подключаем только когда мы не на экране дисков
    val lazyPagingItems: LazyPagingItems<FileItem>? = if (currentPath != null) {
        viewModel.filesFlow.collectAsLazyPagingItems()
    } else null

    val context = LocalContext.current

    BackHandler(enabled = true) {
        viewModel.goBack()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    // Если currentPath == null, показываем заголовок "Диски", иначе - текущий путь
                    Text(currentPath ?: "Диски")
                },
                actions = {
                    // Кнопка фильтра имеет смысл только внутри директории
                    if (currentPath != null) {
                        IconButton(onClick = { viewModel.setFilter("") }) {
                            Icon(Icons.Default.FilterAlt, "Filter")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        LaunchedEffect(messages) {
            if (messages.isNotEmpty()) {
                Toast.makeText(context, messages.last(), Toast.LENGTH_SHORT).show()
            }
        }

        if (currentPath == null) { // ЭКРАН ДИСКОВ
            when (val state = disksState) {
                is DisksUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is DisksUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(state.message, color = MaterialTheme.colorScheme.error)
                            Button(onClick = { viewModel.loadDisks() }) {
                                Text("Повторить")
                            }
                        }
                    }
                }
                is DisksUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.padding(innerPadding).fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(state.disks) { disk ->
                            DiskCard(
                                disk = disk,
                                onClick = {
                                    // При клике переходим в корень диска
                                    viewModel.openDir(disk.mountPoint)
                                }
                            )
                        }
                    }
                }
            }
        } else if (lazyPagingItems != null) { // ЭКРАН ФАЙЛОВ
            LazyColumn(
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(
                    count = lazyPagingItems.itemCount,
                    key = lazyPagingItems.itemKey { it.path }
                ) { index ->
                    val fileItem = lazyPagingItems[index]
                    fileItem?.let { file ->
                        FileCard(
                            file = file,
                            onClick = {
                                if (file.isDirectory) viewModel.openDir(file.path)
                            }
                        )
                    }
                }

                if (lazyPagingItems.loadState.append is LoadState.Loading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        }
                    }
                }
            }

            if (lazyPagingItems.loadState.refresh is LoadState.Loading) {
                Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }

            val refreshState = lazyPagingItems.loadState.refresh
            if (refreshState is LoadState.Error) {
                Box(
                    modifier = Modifier.padding(innerPadding).fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Ошибка загрузки: ${refreshState.error.localizedMessage ?: "Неизвестная ошибка"}",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp)
                        )
                        Button(onClick = { lazyPagingItems.retry() }) {
                            Text("Повторить")
                        }
                    }
                }
            }
        }
    }
}