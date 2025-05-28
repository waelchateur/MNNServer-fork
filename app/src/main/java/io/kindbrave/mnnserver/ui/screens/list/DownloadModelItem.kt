package io.kindbrave.mnnserver.ui.screens.list

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.alibaba.mls.api.ModelItem
import io.kindbrave.mnnserver.R
import io.kindbrave.mnnserver.utils.ModelUtils
import kotlin.collections.get

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DownloadModelItemView(model: ModelItem, loaded: Boolean) {
    val viewModel: ModelListViewModel = hiltViewModel()
    viewModel.updateDownloadState(model)
    val downloadState by viewModel.downloadStateMap[model.modelId]!!.collectAsState()
    val tags = remember(model) { model.getTags() }
    val modelName = remember(model) { model.modelName ?: "" }
    val drawableId = remember(model) { ModelUtils.getDrawableId(modelName) }

    var showConfigDialog by remember { mutableStateOf(false) }

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
        ModelIcon(drawableId)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 4.dp)
        ) {
            ModelName(modelName, tags, downloadState)
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
                        modifier = Modifier.align(Alignment.CenterVertically).size(20.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.start),
                            contentDescription = stringResource(R.string.load),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { showConfigDialog = true },
                        modifier = Modifier.align(Alignment.CenterVertically).size(20.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.config),
                            contentDescription = stringResource(R.string.model_config),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { viewModel.deleteDownloadModel(model) },
                        modifier = Modifier.align(Alignment.CenterVertically).size(20.dp)
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

    if (showConfigDialog) {
        ModelConfigBottomSheet(
            model.modelId!!,
            viewModel.getModelPath(model.modelId!!),
            onDismissRequest = {
                showConfigDialog = false
            }
        )
    }
}

@Composable
private fun ModelIcon(drawableId: Int) {
    Icon(
        painter = painterResource(if (drawableId == 0) R.drawable.unknown else drawableId),
        contentDescription = stringResource(R.string.download_models),
        modifier = Modifier.size(40.dp),
        tint = Color.Unspecified
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ModelName(modelName: String, tags: List<String>, downloadState: ModelDownloadState) {
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

@Composable
private fun DownloadProgress(
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