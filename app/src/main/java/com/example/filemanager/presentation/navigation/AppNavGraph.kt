package com.example.filemanager.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.filemanager.presentation.ui.srceen.AuthScreen
import com.example.filemanager.presentation.ui.srceen.ManageScreen
import com.example.filemanager.presentation.viewmodel.AuthViewModel
import com.example.filemanager.presentation.viewmodel.ManageViewModel

@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel,
    manageViewModel: ManageViewModel
) {
    NavHost(
        navController = navController,
        startDestination = "auth",
        builder = {
            composable("auth") {
                AuthScreen(authViewModel)
            }

            composable("home") {
                ManageScreen(manageViewModel)
            }
        }
    )
}