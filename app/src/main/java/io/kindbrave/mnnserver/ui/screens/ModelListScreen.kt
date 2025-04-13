package io.kindbrave.mnnserver.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.kindbrave.mnnserver.R
import io.kindbrave.mnnserver.model.ModelManager
import io.kindbrave.mnnserver.ui.components.ModelNameDialog
import io.kindbrave.mnnserver.viewmodel.ModelListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelListScreen(
    navController: NavController,
    viewModel: ModelListViewModel = viewModel()
) {
    val modelList by viewModel.modelList.collectAsState()
    
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
        
        ModelNameDialog()
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