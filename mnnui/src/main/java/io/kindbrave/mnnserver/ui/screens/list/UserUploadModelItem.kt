package io.kindbrave.mnnserver.ui.screens.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import io.kindbrave.mnnserver.R
import io.kindbrave.mnn.webserver.repository.UserUploadModelRepository.ModelInfo

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UserUploadModelItemView(model: ModelInfo, loaded: Boolean) {
    val viewModel: ModelListViewModel = hiltViewModel()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.local),
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = Color.Unspecified
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 4.dp)
        ) {
            Text(
                text = model.name,
                fontSize = 16.sp,
                maxLines = 1
            )
        }

        if (loaded) {
            IconButton(
                onClick = { viewModel.unloadUserUploadModel(model) },
                modifier = Modifier.align(Alignment.CenterVertically)
            ) {
                Icon(
                    painter = painterResource(R.drawable.stop),
                    contentDescription = stringResource(R.string.unload),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        } else {
            IconButton(
                onClick = { viewModel.loadUserUploadModel(model) },
                modifier = Modifier.align(Alignment.CenterVertically)
            ) {
                Icon(
                    painter = painterResource(R.drawable.start),
                    contentDescription = stringResource(R.string.load),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            IconButton(
                onClick = { viewModel.deleteUserUploadModel(model) },
                modifier = Modifier.align(Alignment.CenterVertically)
            ) {
                Icon(
                    painter = painterResource(R.drawable.delete),
                    contentDescription = stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}