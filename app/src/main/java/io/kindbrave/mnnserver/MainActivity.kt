package io.kindbrave.mnnserver

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.k2fsa.sherpa.mnn.OfflineTts
import com.k2fsa.sherpa.mnn.OfflineTtsConfig
import com.k2fsa.sherpa.mnn.OfflineTtsKokoroModelConfig
import com.k2fsa.sherpa.mnn.OfflineTtsModelConfig
import com.k2fsa.sherpa.mnn.OfflineTtsVitsModelConfig
import com.taobao.meta.avatar.tts.TtsService
import dagger.hilt.android.AndroidEntryPoint
import io.kindbrave.mnnserver.navigation.Navigation
import io.kindbrave.mnnserver.ui.theme.MNNServerTheme
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MNNServerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Navigation()
                }
            }
        }
    }
}