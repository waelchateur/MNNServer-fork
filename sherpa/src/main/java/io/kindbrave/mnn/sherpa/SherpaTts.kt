package io.kindbrave.mnn.sherpa

import com.k2fsa.sherpa.mnn.GeneratedAudio
import com.k2fsa.sherpa.mnn.OfflineTts
import com.k2fsa.sherpa.mnn.OfflineTtsConfig
import com.k2fsa.sherpa.mnn.OfflineTtsKokoroModelConfig
import com.k2fsa.sherpa.mnn.OfflineTtsModelConfig
import com.k2fsa.sherpa.mnn.OfflineTtsVitsModelConfig
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SherpaTts {

    private lateinit var tts: OfflineTts
    @Volatile
    private var hasLoading = false
    private val initComplete = CompletableDeferred<Boolean>()

    suspend fun init(config: OfflineTtsConfig) {
        if (initComplete.isCompleted) {
            return
        }
        if (hasLoading) {
            initComplete.await()
        }
        withContext(Dispatchers.IO) {
            tts = OfflineTts(config = config)
        }
        initComplete.complete(true)
    }

    fun process(text: String): GeneratedAudio? {
        if (!initComplete.isCompleted) {
            return null
        }
        val audio = tts.generate(text=text, sid = 47, speed = 1.0f)
        return audio
    }

    fun release() {
        tts.release()
    }

    companion object {
        init {
            System.loadLibrary("sherpa-mnn-jni")
        }
    }
}