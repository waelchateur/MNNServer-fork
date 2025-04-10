package io.kindbrave.mnnserver.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.kindbrave.mnnserver.R
import io.kindbrave.mnnserver.viewmodel.MainViewModel

@Composable
fun ModelNameDialog(
    viewModel: MainViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val showDialog by viewModel.showModelNameDialog
    val modelNameInput by viewModel.modelNameInput
    
    if (showDialog) {
        Dialog(onDismissRequest = { viewModel.showModelNameDialog.value = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.enter_model_name),
                        style = MaterialTheme.typography.titleLarge
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = modelNameInput,
                        onValueChange = { viewModel.modelNameInput.value = it },
                        label = { Text(stringResource(R.string.model_name)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { viewModel.showModelNameDialog.value = false }
                        ) {
                            Text(stringResource(R.string.cancel))
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Button(
                            onClick = {
                                if (modelNameInput.isNotBlank()) {
                                    viewModel.importModelWithName(modelNameInput)
                                    viewModel.showModelNameDialog.value = false
                                }
                            },
                            enabled = modelNameInput.isNotBlank()
                        ) {
                            Text(stringResource(R.string.imports))
                        }
                    }
                }
            }
        }
    }
} 