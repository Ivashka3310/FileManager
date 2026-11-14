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
import com.example.filemanager.domain.FileRepository
import com.example.filemanager.domain.SessionStorage
import com.example.filemanager.domain.usecase.ConnectViaQrDataUseCase
import com.example.filemanager.domain.usecase.GetFilesPagedUseCase
import com.example.filemanager.presentation.navigation.AppNavGraph
import com.example.filemanager.presentation.theme.FileManagerTheme
import com.example.filemanager.presentation.ui.srceen.ManageScreen
import com.example.filemanager.presentation.viewmodel.AuthViewModel
import com.example.filemanager.presentation.viewmodel.ManageViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //enableEdgeToEdge()
        
        val sessionStorage: SessionStorage = SessionStorageImpl(applicationContext)
        val fileRepository: FileRepository = FileRepositoryImpl(
            client = KtorClient.client,
            sessionStorage = sessionStorage
        )

        val connectViaQrDataUseCase = ConnectViaQrDataUseCase(fileRepository)
        val getFilesPagedUseCase = GetFilesPagedUseCase(fileRepository)

        val authViewModel: AuthViewModel by viewModels {
            viewModelFactory {
                initializer { AuthViewModel(connectViaQrDataUseCase) }
            }
        }

        val manageViewModel: ManageViewModel by viewModels {
            viewModelFactory {
                initializer { ManageViewModel(getFilesPagedUseCase) }
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
                    manageViewModel
                )
            }
        }
    }
}