package io.kindbrave.mnnserver.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.kindbrave.mnnserver.R
import io.kindbrave.mnnserver.service.WebServerService

@Composable
fun ServerStatusCard(
    serverStatus: ServerStatus,
    port: Int,
    loadedModelsCount: Int,
    onStartServer: () -> Unit,
    onStopServer: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = stringResource(R.string.server_status),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            val statusText = when (serverStatus) {
                is ServerStatus.Running -> stringResource(R.string.server_running, port)
                is ServerStatus.Starting -> stringResource(R.string.server_starting)
                is ServerStatus.Stopped -> stringResource(R.string.server_stopped)
                is ServerStatus.Error -> stringResource(R.string.server_error, serverStatus.message)
            }

            val statusColor = when (serverStatus) {
                is ServerStatus.Running -> MaterialTheme.colorScheme.primary
                is ServerStatus.Starting -> MaterialTheme.colorScheme.primary
                is ServerStatus.Stopped -> MaterialTheme.colorScheme.error
                is ServerStatus.Error -> MaterialTheme.colorScheme.error
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            color = statusColor,
                            shape = RoundedCornerShape(6.dp)
                        )
                )
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = statusColor
                )
            }

            if (serverStatus is ServerStatus.Running) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.running_models, loadedModelsCount),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = onStartServer,
                    enabled = serverStatus !is ServerStatus.Running && serverStatus!is ServerStatus.Starting
                ) {
                    Text(stringResource(R.string.start_server))
                }

                OutlinedButton(
                    onClick = onStopServer,
                    enabled = serverStatus is ServerStatus.Running
                ) {
                    Text(stringResource(R.string.stop_server))
                }
            }
        }
    }
}