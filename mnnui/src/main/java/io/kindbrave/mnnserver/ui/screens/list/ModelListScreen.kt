package io.kindbrave.mnnserver.ui.screens.list

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import io.kindbrave.mnnserver.R
import io.kindbrave.mnnserver.ui.components.LoadingDialog
import io.kindbrave.mnnserver.ui.components.ModelNameDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelListScreen(navController: NavHostController) {
    val viewModel: ModelListViewModel = hiltViewModel()
    val downloadModels by viewModel.downloadModels.collectAsState()
    val customDownloadModels by viewModel.customDownloadModels.collectAsState()
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

    var modelFilter: ModelFilter by remember { mutableStateOf(ModelFilter.All) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.model_list)) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    var showDropdown by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showDropdown = true }) {
                            Icon(
                                painter = painterResource(R.drawable.filter),
                                contentDescription = stringResource(R.string.filter_model)
                            )
                        }

                        DropdownMenu(
                            expanded = showDropdown,
                            onDismissRequest = { showDropdown = false }
                        ) {
                            DropdownMenuItem(
                                onClick = {
                                    showDropdown = false
                                    modelFilter = ModelFilter.All
                                },
                                text = {
                                    Text(stringResource(R.string.all))
                                }
                            )
                            DropdownMenuItem(
                                onClick = {
                                    showDropdown = false
                                    modelFilter = ModelFilter.Chat
                                },
                                text = {
                                    Text(stringResource(R.string.chat))
                                }
                            )
                            DropdownMenuItem(
                                onClick = {
                                    showDropdown = false
                                    modelFilter = ModelFilter.Embedding
                                },
                                text = {
                                    Text(stringResource(R.string.embedding))
                                }
                            )
                            DropdownMenuItem(
                                onClick = {
                                    showDropdown = false
                                    modelFilter = ModelFilter.Asr
                                },
                                text = {
                                    Text(stringResource(R.string.asr))
                                }
                            )
                            DropdownMenuItem(
                                onClick = {
                                    showDropdown = false
                                    modelFilter = ModelFilter.Custom
                                },
                                text = {
                                    Text(stringResource(R.string.custom_model))
                                }
                            )
                        }
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
            val filteredModels = when (modelFilter) {
                ModelFilter.All -> customDownloadModels + downloadModels
                ModelFilter.Chat -> customDownloadModels.filter { it.getTags().contains("embedding").not() } +
                        downloadModels.filter { it.getTags().contains("embedding").not() }
                ModelFilter.Embedding -> customDownloadModels.filter { it.getTags().contains("embedding") } +
                        downloadModels.filter { it.getTags().contains("embedding") }
                ModelFilter.Asr -> customDownloadModels.filter { it.getTags().contains("asr") } +
                        downloadModels.filter { it.getTags().contains("asr") }
                ModelFilter.Custom -> customDownloadModels // 不加 downloadModels
            }

            // 按 loaded 排序，并记录类型（Custom or Official）
            val combinedSortedModels = filteredModels
                .map { model ->
                    val type = if (customDownloadModels.contains(model)) DownloadModelType.CUSTOM else DownloadModelType.OFFICIAL
                    Triple(model, loadedModels.contains(model.modelId), type)
                }
                .sortedByDescending { it.second } // 先显示 loaded 的模型

            items(combinedSortedModels.size) { index ->
                val (model, isLoaded, type) = combinedSortedModels[index]
                DownloadModelItemView(
                    model = model,
                    loaded = isLoaded,
                    type = type
                )
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
    val getCustomDownloadModelState by viewModel.getCustomDownloadModelState.collectAsState()
    LaunchedEffect(Unit) {
        when (getCustomDownloadModelState) {
            is GetDownloadModelState.Error -> {
                snackbarHostState.showSnackbar((getDownloadModelState as GetDownloadModelState.Error).message)
            }
            else -> {}
        }
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

sealed class ModelFilter  {
    object All : ModelFilter()
    object Chat : ModelFilter()
    object Embedding : ModelFilter()
    object Asr: ModelFilter()
    object Custom: ModelFilter()
}