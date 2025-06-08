package io.kindbrave.mnnserver.ui.screens.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import io.kindbrave.mnnserver.R

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