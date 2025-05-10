package io.kindbrave.mnnserver.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.kindbrave.mnnserver.R
import io.kindbrave.mnnserver.ui.components.PortSettingsDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = viewModel()
) {
    val serverPort by viewModel.serverPort.collectAsState()
    var showPortSettingsDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.port_settings)) },
                supportingContent = { Text("$serverPort") },
                leadingContent = { Icon(
                    modifier = Modifier.size(24.dp),
                    painter = painterResource(R.drawable.web),
                    contentDescription = null
                ) },
                modifier = Modifier.clickable { showPortSettingsDialog = true }
            )
            
            ListItem(
                headlineContent = { Text(stringResource(R.string.model_list)) },
                leadingContent = { Icon(
                    modifier = Modifier.size(24.dp),
                    painter = painterResource(R.drawable.list),
                    contentDescription = null
                ) },
                modifier = Modifier.clickable { navController.navigate("settings/model_list") }
            )
            
            ListItem(
                headlineContent = { Text(stringResource(R.string.service_logs)) },
                leadingContent = { Icon(
                    modifier = Modifier.size(24.dp),
                    painter = painterResource(R.drawable.log),
                    contentDescription = null
                ) },
                modifier = Modifier.clickable { navController.navigate("settings/logs") }
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.download_models)) },
                leadingContent = { Icon(
                    modifier = Modifier.size(24.dp),
                    painter = painterResource(R.drawable.download),
                    contentDescription = null
                ) },
                modifier = Modifier.clickable { navController.navigate("settings/download") }
            )
        }
        
        if (showPortSettingsDialog) {
            PortSettingsDialog(
                currentPort = serverPort,
                onDismiss = { showPortSettingsDialog = false },
                onSave = { port ->
                    viewModel.updateServerPort(port)
                    showPortSettingsDialog = false
                }
            )
        }
    }
} 