package io.kindbrave.mnnserver.ui.screens.model

import android.view.WindowInsets
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.kindbrave.mnnserver.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelConfigBottomSheet(
    modelId: String,
    modelPath: String,
    onDismissRequest: () -> Unit
) {
    val viewModel: ModelConfigViewModel = viewModel()
    val sheetState = rememberModalBottomSheetState()
    val modelConfig by viewModel.modelConfig.collectAsState()
    var thinkingMode by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        viewModel.initModelConfig(modelId, modelPath)
    }

    LaunchedEffect(modelConfig) {
        thinkingMode = modelConfig.thinkingMode == true
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = {
                    viewModel.setThinkingMode(thinkingMode.not())
                },
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = if (thinkingMode) stringResource(R.string.thinking_mode) else stringResource(R.string.no_thinking_mode),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}
