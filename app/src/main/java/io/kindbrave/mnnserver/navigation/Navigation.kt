package io.kindbrave.mnnserver.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.kindbrave.mnnserver.ui.screens.download.DownloadScreen
import io.kindbrave.mnnserver.ui.screens.log.LogsScreen
import io.kindbrave.mnnserver.ui.screens.main.MainScreen
import io.kindbrave.mnnserver.ui.screens.modellist.ModelListScreen
import io.kindbrave.mnnserver.ui.screens.settings.SettingsScreen

@Composable
fun Navigation() {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = "main"
    ) {
        composable("main") {
            MainScreen(navController = navController)
        }
        
        composable("settings") {
            SettingsScreen(navController = navController)
        }
        
        composable("settings/model_list") {
            ModelListScreen(navController = navController)
        }
        
        composable("settings/logs") {
            LogsScreen(navController = navController)
        }

        composable("settings/download") {
            DownloadScreen(navController = navController)
        }
    }
} 