package io.kindbrave.mnnserver.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.kindbrave.mnnserver.ui.screens.download.ModelListScreen
import io.kindbrave.mnnserver.ui.screens.log.LogsScreen
import io.kindbrave.mnnserver.ui.screens.main.MainScreen
import io.kindbrave.mnnserver.ui.screens.settings.SettingsScreen

@Composable
fun Navigation() {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = "main",
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(300))
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(300))
        },
        popEnterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(300))
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(300))
        }
    ) {
        composable("main") {
            MainScreen(navController = navController)
        }
        
        composable("settings") {
            SettingsScreen(navController = navController)
        }
        
        composable("settings/logs") {
            LogsScreen(navController = navController)
        }

        composable("model_list") {
            ModelListScreen(navController = navController)
        }
    }
} 