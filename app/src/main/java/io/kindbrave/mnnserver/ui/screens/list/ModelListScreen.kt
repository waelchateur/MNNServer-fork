package io.kindbrave.mnnserver.ui.screens.list

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.alibaba.mls.api.ModelItem
import io.kindbrave.mnnserver.R
import io.kindbrave.mnnserver.repository.model.UserUploadModelRepository.ModelInfo
import io.kindbrave.mnnserver.ui.components.LoadingDialog
import io.kindbrave.mnnserver.ui.components.ModelNameDialog
import io.kindbrave.mnnserver.utils.ModelUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelListScreen(navController: NavHostController) {
    val viewModel: ModelListViewModel = hiltViewModel()
    val downloadModels by viewModel.downloadModels.collectAsState()
    val userUploadModels by viewModel.userUploadModels.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var uploadFolderUri: Uri? by rememberSaveable { mutableStateOf(null) }
    var showEnterModelNameDialog by rememberSaveable { mutableStateOf(false) }

    val loadedModels by viewModel.loadedModels.collectAsState()

    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uploadFolderUri = uri
        showEnterModelNameDialog = true
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.model_list)) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
//        floatingActionButton = {
//            FloatingActionButton(onClick = {
//                folderPicker.launch(null)
//            }) {
//                Icon(
//                    Icons.Default.Add,
//                    contentDescription = stringResource(R.string.import_model)
//                )
//            }
//        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
//            items(userUploadModels.size) { index ->
//                UserUploadModelItemView(model = userUploadModels[index], loadedModels.contains(userUploadModels[index].id))
//            }
            items(downloadModels.size) { index ->
                DownloadModelItemView(model = downloadModels[index], loadedModels.contains(downloadModels[index].modelId))
            }
        }
        if (showEnterModelNameDialog) {
            ModelNameDialog(
                onDismiss = {
                    uploadFolderUri = null
                    showEnterModelNameDialog = false
                },
                onConfirm = {
                    showEnterModelNameDialog = false
                    viewModel.onModelNameEntered(it, uploadFolderUri)
                }
            )
        }
    }

    val getDownloadModelState by viewModel.getDownloadModelState.collectAsState()
    when (getDownloadModelState) {
        is GetDownloadModelState.Error -> {
            LaunchedEffect(Unit) {
                snackbarHostState.showSnackbar((getDownloadModelState as GetDownloadModelState.Error).message)
            }
        }
        else -> {}
    }

    val loadingState by viewModel.loadingState.collectAsState()
    when (loadingState) {
        is LoadingState.Error -> {
            LaunchedEffect(Unit) {
                snackbarHostState.showSnackbar((loadingState as LoadingState.Error).message)
            }
        }
        is LoadingState.Loading -> {
            val title = (loadingState as LoadingState.Loading).message
            LoadingDialog(title)
        }
        else -> Unit
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DownloadModelItemView(model: ModelItem, loaded: Boolean) {
    val viewModel: ModelListViewModel = hiltViewModel()
    viewModel.updateDownloadState(model)
    val downloadState by viewModel.downloadStateMap[model.modelId]!!.collectAsState()
    val tags = remember(model) { model.getTags() }
    val modelName = remember(model) { model.modelName ?: "" }
    val drawableId = remember(model) { ModelUtils.getDrawableId(modelName) }

    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }


    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(if (drawableId == 0) R.drawable.unknown else drawableId),
            contentDescription = stringResource(R.string.download_models),
            modifier = Modifier.size(40.dp),
            tint = Color.Unspecified
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 4.dp)
        ) {
            Text(
                text = modelName,
                fontSize = 16.sp,
                maxLines = 1
            )

            if (tags.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    tags.forEach { tag ->
                        TagChip(tag)
                    }
                }
            }
            DownloadProgress(downloadState)
        }

        when (downloadState) {
            is ModelDownloadState.Start, is ModelDownloadState.Progress -> {
                IconButton(
                    onClick = { viewModel.pauseDownload(model) },
                    modifier = Modifier.align(Alignment.CenterVertically)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.pause),
                        contentDescription = stringResource(R.string.download_models)
                    )
                }
            }
            is ModelDownloadState.Paused, is ModelDownloadState.Idle, is ModelDownloadState.Failed -> {
                IconButton(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            if (ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                        viewModel.startDownload(model)
                    },
                    modifier = Modifier.align(Alignment.CenterVertically)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.download),
                        contentDescription = stringResource(R.string.download_models)
                    )
                }
            }
            is ModelDownloadState.Finished -> {
                if (loaded) {
                    IconButton(
                        onClick = { viewModel.unloadDownloadModel(model) },
                        modifier = Modifier.align(Alignment.CenterVertically)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.stop),
                            contentDescription = stringResource(R.string.load),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    IconButton(
                        onClick = { viewModel.loadDownloadModel(model) },
                        modifier = Modifier.align(Alignment.CenterVertically)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.start),
                            contentDescription = stringResource(R.string.load),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { viewModel.deleteDownloadModel(model) },
                        modifier = Modifier.align(Alignment.CenterVertically)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.delete),
                            contentDescription = stringResource(R.string.delete),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            else -> Unit
        }
    }
}

@Composable
fun TagChip(tag: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.padding(2.dp)
    ) {
        Text(
            text = tag,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            maxLines = 1
        )
    }
}

@Composable
fun DownloadProgress(
    downloadState: ModelDownloadState
) {
    var lastProgress by remember { mutableFloatStateOf(0.0f) }
    LaunchedEffect(downloadState) {
        if (downloadState is ModelDownloadState.Progress) {
            lastProgress = downloadState.progress.toFloat()
        }
        if (downloadState is ModelDownloadState.Paused) {
            if (downloadState.progress != -1.0) {
                lastProgress = downloadState.progress.toFloat()
            }
        }
    }
    when (downloadState) {
        is ModelDownloadState.Progress,
        is ModelDownloadState.Start,
        is ModelDownloadState.Paused-> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LinearProgressIndicator(
                    modifier = Modifier.weight(1f),
                    progress = { lastProgress }
                )
                Text(
                    text = "${(lastProgress * 100).toInt()}%",
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
        else -> Unit
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UserUploadModelItemView(model: ModelInfo, loaded: Boolean) {
    val viewModel: ModelListViewModel = hiltViewModel()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.local),
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = Color.Unspecified
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 4.dp)
        ) {
            Text(
                text = model.name,
                fontSize = 16.sp,
                maxLines = 1
            )
        }

        if (loaded) {
            IconButton(
                onClick = { viewModel.unloadUserUploadModel(model) },
                modifier = Modifier.align(Alignment.CenterVertically)
            ) {
                Icon(
                    painter = painterResource(R.drawable.stop),
                    contentDescription = stringResource(R.string.unload),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        } else {
            IconButton(
                onClick = { viewModel.loadUserUploadModel(model) },
                modifier = Modifier.align(Alignment.CenterVertically)
            ) {
                Icon(
                    painter = painterResource(R.drawable.start),
                    contentDescription = stringResource(R.string.load),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            IconButton(
                onClick = { viewModel.deleteUserUploadModel(model) },
                modifier = Modifier.align(Alignment.CenterVertically)
            ) {
                Icon(
                    painter = painterResource(R.drawable.delete),
                    contentDescription = stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}