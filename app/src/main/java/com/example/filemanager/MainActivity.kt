package com.example.filemanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.rememberNavController
import com.example.filemanager.data.FileRepositoryImpl
import com.example.filemanager.data.KtorClient
import com.example.filemanager.data.SessionStorageImpl
import com.example.filemanager.data.local.FileDao
import com.example.filemanager.data.local.FileDatabase
import com.example.filemanager.data.local.FileLocalDataSource
import com.example.filemanager.data.local.FileLocalMapper
import com.example.filemanager.data.remote.FileRemoteDataSource
import com.example.filemanager.data.remote.FileRemoteDataSourceImpl
import com.example.filemanager.domain.FileRepository
import com.example.filemanager.domain.SessionStorage
import com.example.filemanager.domain.usecase.ConnectViaQrDataUseCase
import com.example.filemanager.domain.usecase.CreateFileUseCase
import com.example.filemanager.domain.usecase.CreateZipUseCase
import com.example.filemanager.domain.usecase.DeleteFilesUseCase
import com.example.filemanager.domain.usecase.GetJsonFileContentUseCase
import com.example.filemanager.domain.usecase.GetPagedFilesUseCase
import com.example.filemanager.domain.usecase.GetLocalDisksUseCase
import com.example.filemanager.domain.usecase.GetXmlFileContentUseCase
import com.example.filemanager.domain.usecase.RenameFileUseCase
import com.example.filemanager.presentation.navigation.AppNavGraph
import com.example.filemanager.presentation.theme.FileManagerTheme
import com.example.filemanager.presentation.viewmodel.AuthViewModel
import com.example.filemanager.presentation.viewmodel.JsonViewerViewModel
import com.example.filemanager.presentation.viewmodel.ManageViewModel
import com.example.filemanager.presentation.viewmodel.XmlViewerViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //enableEdgeToEdge()

        val fileRemoteDataSource: FileRemoteDataSource = FileRemoteDataSourceImpl(
            client = KtorClient.client
        )

        val database = FileDatabase.getDatabase(applicationContext)
        val dao: FileDao = database.fileDao()
        val localMapper = FileLocalMapper()
        val localDataSource = FileLocalDataSource(dao)

        val sessionStorage: SessionStorage = SessionStorageImpl(applicationContext)

        val fileRepository: FileRepository = FileRepositoryImpl(
            fileRemoteDataSource = fileRemoteDataSource,
            localDataSource = localDataSource,
            sessionStorage = sessionStorage,
            mapper = localMapper
        )

        val connectViaQrDataUseCase = ConnectViaQrDataUseCase(fileRepository)
        val getPagedFilesUseCase = GetPagedFilesUseCase(fileRepository)
        val getLocalDisksUseCase = GetLocalDisksUseCase(fileRepository)
        val createFileUseCase = CreateFileUseCase(fileRepository)
        val renameFileUseCase = RenameFileUseCase(fileRepository)
        val deleteFilesUseCase = DeleteFilesUseCase(fileRepository)
        val getJsonFileContentUseCase = GetJsonFileContentUseCase(fileRepository)
        val getXmlFileContentUseCase = GetXmlFileContentUseCase(fileRepository)
        val createZipUseCase = CreateZipUseCase(fileRepository)

        val authViewModel: AuthViewModel by viewModels {
            viewModelFactory {
                initializer { AuthViewModel(connectViaQrDataUseCase) }
            }
        }

        val manageViewModel: ManageViewModel by viewModels {
            viewModelFactory {
                initializer {
                    ManageViewModel(
                        getPagedFilesUseCase = getPagedFilesUseCase,
                        getLocalDisksUseCase = getLocalDisksUseCase,
                        createFileUseCase = createFileUseCase,
                        renameFileUseCase = renameFileUseCase,
                        deleteFilesUseCase = deleteFilesUseCase,
                        createZipUseCase = createZipUseCase
                    )
                }
            }
        }

        val jsonViewerViewModel: JsonViewerViewModel by viewModels {
            viewModelFactory {
                initializer { JsonViewerViewModel(getJsonFileContentUseCase) }
            }
        }

        val xmlViewerViewModel: XmlViewerViewModel by viewModels {
            viewModelFactory {
                initializer { XmlViewerViewModel(getXmlFileContentUseCase) }
            }
        }

        setContent {
            val navController = rememberNavController()
            val accessToken by sessionStorage.accessToken.collectAsState(initial = "LOADING")

            LaunchedEffect(accessToken) {
                when {
                    accessToken == "LOADING" -> {
                        // Ждем, пока DataStore прочитает данные с диска (занимает ~10 мс)
                    }
                    !accessToken.isNullOrEmpty() -> {
                        navController.navigate("home") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                    else -> {
                        navController.navigate("auth") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            }

            FileManagerTheme {
                AppNavGraph(
                    navController,
                    authViewModel,
                    manageViewModel,
                    jsonViewerViewModel,
                    xmlViewerViewModel
                )
            }
        }
    }
}