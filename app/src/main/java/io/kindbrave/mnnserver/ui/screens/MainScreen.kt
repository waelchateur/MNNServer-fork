package io.kindbrave.mnnserver.ui.screens

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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.kindbrave.mnnserver.R
import io.kindbrave.mnnserver.service.WebServerService
import io.kindbrave.mnnserver.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavController,
    viewModel: MainViewModel = viewModel()
) {
    val serverStatus by viewModel.serverStatus.collectAsState()
    val loadedModelsCount by viewModel.loadedModelsCount.collectAsState()
    val deviceInfo by viewModel.deviceInfo.collectAsState()
    
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
                loadedModelsCount = loadedModelsCount,
                onStartServer = { viewModel.startServer() },
                onStopServer = { viewModel.stopServer() }
            )
            DeviceInfoCard(deviceInfo = deviceInfo)
        }
    }
}

@Composable
fun DeviceInfoCard(deviceInfo: DeviceInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = stringResource(R.string.device_info),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.memory),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "${deviceInfo.availableMemory}MB / ${deviceInfo.totalMemory}MB",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Column {
                    Text(
                        text = stringResource(R.string.storage),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "${deviceInfo.availableStorage}GB / ${deviceInfo.totalStorage}GB",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun ServerStatusCard(
    serverStatus: WebServerService.ServerStatus,
    loadedModelsCount: Int,
    onStartServer: () -> Unit,
    onStopServer: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = stringResource(R.string.server_status),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            val statusText = when (serverStatus) {
                is WebServerService.ServerStatus.Running -> stringResource(R.string.server_running, serverStatus.port)
                is WebServerService.ServerStatus.Stopped -> stringResource(R.string.server_stopped)
                is WebServerService.ServerStatus.Error -> stringResource(R.string.server_error, serverStatus.message)
            }
            
            val statusColor = when (serverStatus) {
                is WebServerService.ServerStatus.Running -> MaterialTheme.colorScheme.primary
                is WebServerService.ServerStatus.Stopped -> MaterialTheme.colorScheme.error
                is WebServerService.ServerStatus.Error -> MaterialTheme.colorScheme.error
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            color = statusColor,
                            shape = RoundedCornerShape(6.dp)
                        )
                )
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = statusColor
                )
            }
            
            if (serverStatus is WebServerService.ServerStatus.Running) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.running_models, loadedModelsCount),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = onStartServer,
                    enabled = serverStatus !is WebServerService.ServerStatus.Running
                ) {
                    Text(stringResource(R.string.start_server))
                }
                
                OutlinedButton(
                    onClick = onStopServer,
                    enabled = serverStatus is WebServerService.ServerStatus.Running
                ) {
                    Text(stringResource(R.string.stop_server))
                }
            }
        }
    }
}

data class DeviceInfo(
    val availableMemory: Long,
    val totalMemory: Long,
    val availableStorage: Long,
    val totalStorage: Long
)