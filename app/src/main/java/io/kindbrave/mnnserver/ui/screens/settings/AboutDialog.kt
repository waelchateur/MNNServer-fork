package io.kindbrave.mnnserver.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.kindbrave.mnnserver.R

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    val githubUrl = "https://github.com/sunshine0523/MNNServer"
    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text(text = stringResource(R.string.app_name)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.developer),
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    text = "GitHub",
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .clickable { uriHandler.openUri(githubUrl) },
                    color = androidx.compose.ui.graphics.Color.Blue
                )
            }
        },
        confirmButton = { }
    )
}