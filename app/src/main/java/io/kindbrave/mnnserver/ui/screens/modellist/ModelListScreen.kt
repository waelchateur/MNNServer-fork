package io.kindbrave.mnnserver.ui.screens.modellist

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
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
import io.kindbrave.mnnserver.model.ModelManager
import io.kindbrave.mnnserver.ui.components.ModelNameDialog
import io.kindbrave.mnnserver.ui.screens.modellist.ModelListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelListScreen(
    navController: NavController,
    viewModel: ModelListViewModel = viewModel()
) {
    val modelList by viewModel.modelList.collectAsState()
    val importState by viewModel.importState.collectAsState()
    val loadingState by viewModel.loadingState.collectAsState()
    var showNameDialog by remember { mutableStateOf(false) }
    val snackbarHostState = SnackbarHostState()
    
    // 文件选择器
    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.onFolderSelected(it)
            showNameDialog = true
        }
    }
    
    // 监听导入状态
    LaunchedEffect(importState) {
        when (importState) {
            is ModelListViewModel.ImportState.SelectFolder -> {
                folderPicker.launch(null)
            }
            is ModelListViewModel.ImportState.Success -> {
                // 导入成功后重置状态
                viewModel.resetImportState()
            }
            is ModelListViewModel.ImportState.Error -> {
                // 显示错误提示
                snackbarHostState.showSnackbar(
                    message = (importState as ModelListViewModel.ImportState.Error).message,
                    duration = SnackbarDuration.Short
                )
                viewModel.resetImportState()
            }
            else -> {}
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.model_list)) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                ) {
                    Text(data.visuals.message)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            ModelList(
                models = modelList,
                onLoadModel = { viewModel.loadModel(it) },
                onUnloadModel = { viewModel.unloadModel(it) },
                onDeleteModel = { viewModel.deleteModel(it) },
                modifier = Modifier.weight(1f)
            )
        }
        
        // 模型名称输入对话框
        if (showNameDialog) {
            ModelNameDialog(
                onDismiss = { 
                    showNameDialog = false
                    viewModel.resetImportState()
                },
                onConfirm = { name ->
                    showNameDialog = false
                    viewModel.onNameEntered(name)
                }
            )
        }
        
        // 导入进度提示
        if (importState is ModelListViewModel.ImportState.Importing || 
            loadingState is ModelListViewModel.LoadingState.Loading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = when {
                            importState is ModelListViewModel.ImportState.Importing -> 
                                stringResource(R.string.import_model)
                            loadingState is ModelListViewModel.LoadingState.Loading -> 
                                (loadingState as ModelListViewModel.LoadingState.Loading).message
                            else -> ""
                        },
                        style = MaterialTheme.typography.bodyLarge
                    )
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
    var showModelConfigDialog by remember { mutableStateOf(false) }
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
            IconButton(onClick = {
                showModelConfigDialog = true
            }) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = stringResource(R.string.settings),
                )
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

    if (showModelConfigDialog) {
        ModelConfigBottomSheet(model.id, model.path, {
            showModelConfigDialog = false
        })
    }
}