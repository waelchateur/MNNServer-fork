package io.kindbrave.mnnserver.ui.screens.list

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import io.kindbrave.mnnserver.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelConfigBottomSheet(
    modelId: String,
    modelPath: String,
    onDismissRequest: () -> Unit
) {
    val viewModel: ModelConfigViewModel = hiltViewModel()
    val sheetState = rememberModalBottomSheetState()
    val backendOptions = listOf("cpu", "opencl")
    var backendExpand by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.initModelConfig(modelId, modelPath)
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
            SectionCard(title = stringResource(R.string.common_settings)) {
                SettingRow(
                    title = stringResource(R.string.thinking_mode),
                    control = {
                        Switch(
                            checked = viewModel.thinkMode.value,
                            onCheckedChange = { viewModel.setThinkingMode(it) }
                        )
                    },
                    onClick = {
                        viewModel.setThinkingMode(viewModel.thinkMode.value.not())
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            SectionCard(title = stringResource(R.string.advanced_settings)) {
                SettingRow(
                    title = stringResource(R.string.mmap_option),
                    control = {
                        Switch(
                            checked = viewModel.mmapEnabled.value,
                            onCheckedChange = { viewModel.setMMap(it) }
                        )
                    },
                    onClick = {
                        viewModel.setMMap(viewModel.mmapEnabled.value.not())
                    }
                )
                SettingRow(
                    title = stringResource(R.string.backend_type),
                    control = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = viewModel.backend.value,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null
                            )
                            DropdownMenu(
                                expanded = backendExpand,
                                onDismissRequest = { backendExpand = false }
                            ) {
                                backendOptions.forEach { selectionOption ->
                                    DropdownMenuItem(
                                        text = { Text(selectionOption) },
                                        onClick = {
                                            viewModel.setBackend(selectionOption)
                                            backendExpand = false
                                        }
                                    )
                                }
                            }
                        }
                    },
                    onClick = {
                        backendExpand = backendExpand.not()
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun SectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            content()
        }
    }
}

@Composable
fun SettingRow(
    title: String,
    control: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(onClick = { onClick() }),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge
        )

        control()
    }
}