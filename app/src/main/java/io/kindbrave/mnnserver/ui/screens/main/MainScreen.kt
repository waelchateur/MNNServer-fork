package io.kindbrave.mnnserver.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.kindbrave.mnnserver.R
import io.kindbrave.mnnserver.service.WebServerService
import io.kindbrave.mnnserver.ui.screens.main.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavController,
    viewModel: MainViewModel = hiltViewModel()
) {
    val serverStatus by viewModel.serverStatus.collectAsState()
    val port by viewModel.serverPort.collectAsState()
    val loadedModelsCount by viewModel.loadedModelsCount.collectAsState()
    val deviceInfo by viewModel.deviceInfo.collectAsState()

    var showIgnoreBatteryOptimizationDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        stringResource(R.string.main_screen_title),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ServerStatusCard(
                serverStatus = serverStatus,
                port = port,
                loadedModelsCount = loadedModelsCount,
                onStartServer = {
                    if (viewModel.isBatteryOptimizationDisabled().not()) {
                        showIgnoreBatteryOptimizationDialog = true
                    }
                    viewModel.startServer()
                },
                onStopServer = { viewModel.stopServer() }
            )
            DeviceInfoCard(deviceInfo = deviceInfo)
        }

        if (showIgnoreBatteryOptimizationDialog) {
            IgnoreBatteryOptimizationDialog(
                onConfirm = {
                    viewModel.requestIgnoreBatteryOptimizations()
                    showIgnoreBatteryOptimizationDialog = false
                },
                onDismiss = { showIgnoreBatteryOptimizationDialog = false }
            )
        }
    }
}