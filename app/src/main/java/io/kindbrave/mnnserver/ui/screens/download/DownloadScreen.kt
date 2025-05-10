package io.kindbrave.mnnserver.ui.screens.download

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.alibaba.mls.api.ModelItem
import io.kindbrave.mnnserver.R
import io.kindbrave.mnnserver.utils.ModelUtils
import kotlinx.coroutines.flow.MutableStateFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadScreen(navController: NavHostController) {
    val viewModel: DownloadViewModel = hiltViewModel()
    val models by viewModel.models.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.download_models)) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(models.size) { index ->
                ModelItemView(model = models[index], onDownloadClick = {
                    viewModel.onModelItemClick(it)
                })
            }
        }
    }
    Loading(snackbarHostState)
}

@Composable
fun Loading(snackbarHostState: SnackbarHostState) {
    val viewModel: DownloadViewModel = hiltViewModel()
    val loadingModelState by viewModel.loadModelState.collectAsState()
    var loadingModelStateMessage by remember { mutableStateOf("") }
    when (loadingModelState) {
        is LoadingModelState.Error -> {
            loadingModelStateMessage = (loadingModelState as LoadingModelState.Error).message
        }
        else -> {}
    }
    LaunchedEffect(loadingModelStateMessage) {
        if (loadingModelStateMessage.isNotBlank()) {
            snackbarHostState.showSnackbar(loadingModelStateMessage)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ModelItemView(model: ModelItem, onDownloadClick: (ModelItem) -> Unit) {
    val viewModel: DownloadViewModel = hiltViewModel()
    val downloadState by (viewModel.downloadStateMap[model.modelId] ?: MutableStateFlow(ModelDownloadState.Idle)).collectAsState()
    val tags = remember(model) { model.getTags() }
    val modelName = remember(model) { model.modelName ?: "" }
    val drawableId = remember(model) { ModelUtils.getDrawableId(modelName) }
    
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

        if (model.isLocal.not()) {
            IconButton(
                onClick = { onDownloadClick(model) },
                modifier = Modifier.align(Alignment.CenterVertically)
            ) {
                Icon(
                    painter = painterResource(
                        if (downloadState is ModelDownloadState.Start || downloadState is ModelDownloadState.Progress) R.drawable.pause
                        else R.drawable.download
                    ),
                    contentDescription = stringResource(R.string.download_models)
                )
            }
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
    var lastProgress by remember { mutableStateOf(0.0f) }
    when (downloadState) {
        is ModelDownloadState.Progress,
        is ModelDownloadState.Start,
        is ModelDownloadState.Paused-> {
            if (downloadState is ModelDownloadState.Progress) {
                lastProgress = downloadState.progress.progress.toFloat()
            }
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