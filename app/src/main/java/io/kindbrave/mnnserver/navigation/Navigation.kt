package io.kindbrave.mnnserver.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.kindbrave.mnnserver.ui.screens.*
import io.kindbrave.mnnserver.viewmodel.*

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
    }
} 