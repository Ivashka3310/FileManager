package com.example.filemanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.filemanager.data.ConnectionManagerImpl
import com.example.filemanager.domain.ConnectionManager
import com.example.filemanager.presentation.theme.FileManagerTheme
import com.example.filemanager.presentation.ui.screen.ManageScreen.ManageScreen
import com.example.filemanager.presentation.ui.viewmodel.ManageViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //enableEdgeToEdge()
        val connectionManager: ConnectionManager = ConnectionManagerImpl()
        val manageViewModel = ManageViewModel(connectionManager)
        setContent {
            FileManagerTheme {
                ManageScreen(manageViewModel)
            }
        }
    }
}