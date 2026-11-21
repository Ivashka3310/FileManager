package com.example.filemanager.presentation.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.filemanager.presentation.ui.srceen.AuthScreen
import com.example.filemanager.presentation.ui.srceen.JsonViewerScreen
import com.example.filemanager.presentation.ui.srceen.ManageScreen
import com.example.filemanager.presentation.viewmodel.AuthViewModel
import com.example.filemanager.presentation.viewmodel.JsonViewerViewModel
import com.example.filemanager.presentation.viewmodel.ManageViewModel

@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel,
    manageViewModel: ManageViewModel,
    jsonViewerViewModel: JsonViewerViewModel
) {
    NavHost(
        navController = navController,
        startDestination = "auth",
        builder = {
            composable("auth") {
                AuthScreen(authViewModel)
            }

            composable("home") {
                ManageScreen(
                    viewModel = manageViewModel,
                    openJson = { path ->
                        val encodedPath = Uri.encode(path)
                        navController.navigate("open/json?filePath=$encodedPath")
                    }
                )
            }

            composable(
                route = "open/json?filePath={filePath}",
                arguments = listOf(
                    navArgument("filePath") {
                        type = NavType.StringType
                    }
                )
            ) { backStackEntry ->
                val encodedPath = backStackEntry.arguments?.getString("filePath")
                val decodedPath = encodedPath?.let { Uri.decode(it) }

                if (decodedPath != null) {
                    JsonViewerScreen(
                        path = decodedPath,
                        viewModel = jsonViewerViewModel,
                        onBack = { navController.popBackStack() }
                    )
                } else {
                    navController.popBackStack()
                }
            }
        }
    )
}