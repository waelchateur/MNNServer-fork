package io.kindbrave.mnnserver.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import io.kindbrave.mnnserver.R
import io.kindbrave.mnnserver.model.ModelManager
import io.kindbrave.mnnserver.service.WebServerService
import io.kindbrave.mnnserver.ui.components.ModelNameDialog
import io.kindbrave.mnnserver.ui.components.ServerSettingsDialog
import io.kindbrave.mnnserver.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    navController: NavController
) {
    val serverStatus by viewModel.serverStatus.collectAsState()
    val modelList by viewModel.modelList.collectAsState()
    val serverPort by viewModel.serverPort.collectAsState()
    
    var showServerSettingsDialog by remember { mutableStateOf(false) }
    
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
                    IconButton(onClick = { showServerSettingsDialog = true }) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings)
                        )
                    }
                    IconButton(onClick = { navController.navigate("logs") }) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = stringResource(R.string.service_logs)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.importModel() }) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.import_model)
                )
            }
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
                onStartServer = { showServerSettingsDialog = true },
                onStopServer = { viewModel.stopServer() }
            )
            
            Text(
                text = stringResource(R.string.model_list),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            ModelList(
                models = modelList,
                onLoadModel = { viewModel.loadModel(it) },
                onUnloadModel = { viewModel.unloadModel(it) },
                onDeleteModel = { viewModel.deleteModel(it) },
                modifier = Modifier.weight(1f)
            )
        }
        
        if (showServerSettingsDialog) {
            ServerSettingsDialog(
                currentPort = serverPort,
                onDismiss = { showServerSettingsDialog = false },
                onStartServer = { port ->
                    viewModel.startServer(port)
                    showServerSettingsDialog = false
                }
            )
        }

        ModelNameDialog()
    }
}

@Composable
fun ServerStatusCard(
    serverStatus: WebServerService.ServerStatus,
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

@Composable
fun ModelList(
    models: List<ModelManager.ModelInfo>,
    onLoadModel: (ModelManager.ModelInfo) -> Unit,
    onUnloadModel: (ModelManager.ModelInfo) -> Unit,
    onDeleteModel: (ModelManager.ModelInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        if (models.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.no_models),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                items(models) { model ->
                    ModelItem(
                        model = model,
                        onLoadModel = onLoadModel,
                        onUnloadModel = onUnloadModel,
                        onDeleteModel = onDeleteModel
                    )
                    
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }
}

@Composable
fun ModelItem(
    model: ModelManager.ModelInfo,
    onLoadModel: (ModelManager.ModelInfo) -> Unit,
    onUnloadModel: (ModelManager.ModelInfo) -> Unit,
    onDeleteModel: (ModelManager.ModelInfo) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = model.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = model.path,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (model.isLoaded) {
                Text(
                    text = stringResource(R.string.loaded),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        if (model.isLoaded) {
            TextButton(onClick = { onUnloadModel(model) }) {
                Text(stringResource(R.string.unload))
            }
        } else {
            TextButton(onClick = { onLoadModel(model) }) {
                Text(stringResource(R.string.load))
            }
            
            IconButton(onClick = { onDeleteModel(model) }) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}