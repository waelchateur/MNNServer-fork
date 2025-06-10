package io.kindbrave.mnnserver.ui.screens.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.verticalScroll
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
import io.kindbrave.mnnserver.ui.screens.list.ModelListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavController,
    viewModel: MainViewModel = hiltViewModel()
) {
    val serverStatus by viewModel.serverStatus.collectAsState()
    val port by viewModel.serverPort.collectAsState()
    val loadedModels by viewModel.loadedModels.collectAsState()
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
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize(),
        ) {
            item {
                Spacer(modifier = Modifier.height(2.dp))
                ServerStatusCard(
                    serverStatus = serverStatus,
                    port = port,
                    loadedModelsCount = loadedModels.size,
                    onStartServer = {
                        if (viewModel.isBatteryOptimizationDisabled().not()) {
                            showIgnoreBatteryOptimizationDialog = true
                        }
                        viewModel.startServer()
                    },
                    onStopServer = { viewModel.stopServer() }
                )
                Spacer(modifier = Modifier.height(16.dp))
                DeviceInfoCard(deviceInfo = deviceInfo)
                Spacer(modifier = Modifier.height(16.dp))
                RunningModel(models = loadedModels)
            }
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