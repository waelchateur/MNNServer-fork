package io.kindbrave.mnnserver.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.kindbrave.mnnserver.R

@Composable
fun ServerSettingsDialog(
    currentPort: Int,
    onDismiss: () -> Unit,
    onStartServer: (Int) -> Unit
) {
    var portInput by remember { mutableStateOf(currentPort.toString()) }
    var portError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    
    Dialog(onDismissRequest = onDismiss) {
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
                    text = stringResource(R.string.server_settings),
                    style = MaterialTheme.typography.titleLarge
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = portInput,
                    onValueChange = { 
                        portInput = it
                        portError = null
                    },
                    label = { Text(stringResource(R.string.port)) },
                    isError = portError != null,
                    supportingText = portError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = {
                            val port = portInput.toIntOrNull()
                            if (port != null && port in 1024..65535) {
                                onStartServer(port)
                            } else {
                                portError = context.getString(R.string.invalid_port)
                            }
                        }
                    ) {
                        Text(stringResource(R.string.start_server))
                    }
                }
            }
        }
    }
} 