package io.kindbrave.mnn.mnnui.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.elvishew.xlog.BuildConfig
import io.kindbrave.mnnserver.R
import io.kindbrave.mnn.mnnui.ui.components.PortSettingsDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val serverPort by viewModel.serverPort.collectAsState()
    val exportWebPort by viewModel.exportWebPort.collectAsState()
    val startLastRunningModels by viewModel.startLastRunningModels.collectAsState()
    var showPortSettingsDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
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
                    headlineContent = { Text(stringResource(R.string.export_web_port)) },
                    supportingContent = { Text(stringResource(R.string.export_web_port_description))},
                    leadingContent = { Icon(
                        modifier = Modifier.size(24.dp),
                        imageVector = Icons.Outlined.Share,
                        contentDescription = null
                    ) },
                    trailingContent = {
                        Switch(
                            checked = exportWebPort,
                            onCheckedChange = { viewModel.setExportWebPort(it) }
                        )
                    },
                    modifier = Modifier.clickable { viewModel.setExportWebPort(exportWebPort.not()) }
                )

                ListItem(
                    headlineContent = { Text(stringResource(R.string.start_last_running_models)) },
                    supportingContent = { Text(stringResource(R.string.start_last_running_models_description))},
                    leadingContent = { Icon(
                        modifier = Modifier.size(24.dp),
                        imageVector = Icons.Outlined.AccountTree,
                        contentDescription = null
                    ) },
                    trailingContent = {
                        Switch(
                            checked = startLastRunningModels,
                            onCheckedChange = { viewModel.setStartLastRunningModels(it) }
                        )
                    },
                    modifier = Modifier.clickable { viewModel.setStartLastRunningModels(startLastRunningModels.not()) }
                )

                ListItem(
                    headlineContent = { Text(stringResource(R.string.model_list)) },
                    leadingContent = { Icon(
                        modifier = Modifier.size(24.dp),
                        imageVector = Icons.AutoMirrored.Outlined.ListAlt,
                        contentDescription = null
                    ) },
                    modifier = Modifier.clickable { navController.navigate("model_list") }
                )

                if (context.packageName == "io.kindbrave.mnnserver") {
                    DefaultTTSModel()
                }

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
                    headlineContent = { Text(stringResource(R.string.about)) },
                    leadingContent = { Icon(
                        modifier = Modifier.size(24.dp),
                        painter = painterResource(R.drawable.about),
                        contentDescription = null
                    ) },
                    modifier = Modifier.clickable { showAboutDialog = true }
                )
            }
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

        if (showAboutDialog) {
            AboutDialog { showAboutDialog = false }
        }
    }
}

@Composable
private fun DefaultTTSModel() {
    val viewModel: SettingsViewModel = hiltViewModel()
    val allTTSSessions = viewModel.getAllTTSSession()
    val defaultTTSModelId by viewModel.defaultTTSModelId.collectAsState()
    var expend by remember { mutableStateOf(false)  }

    ListItem(
        headlineContent = { Text(stringResource(R.string.default_tts_model)) },
        supportingContent = { Text(stringResource(R.string.default_tts_model_description))},
        leadingContent = { Icon(
            modifier = Modifier.size(24.dp),
            painter = painterResource(R.drawable.tts),
            contentDescription = null
        ) },
        trailingContent = {
            TextButton(
                modifier = Modifier.widthIn(max = 100.dp),
                onClick = { expend = expend.not() }
            ) {
                Text(
                    text = if (defaultTTSModelId.isEmpty()) {
                        stringResource(R.string.not_set) } else {
                        defaultTTSModelId
                    },
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            DropdownMenu(
                expanded = expend,
                onDismissRequest = { expend = false }
            ) {
                allTTSSessions.forEach { session ->
                    DropdownMenuItem(
                        text = {
                            Row {
                                if (session.modelId == defaultTTSModelId) {
                                    Icon(painter = painterResource(R.drawable.done), contentDescription = null)
                                }
                                Text(session.modelId)
                            }
                        },
                        onClick = {
                            viewModel.setDefaultTTSModelId(session.modelId)
                            expend = false
                        }
                    )
                }
            }
        },
        modifier = Modifier.clickable {
            expend = expend.not()
        }
    )
}