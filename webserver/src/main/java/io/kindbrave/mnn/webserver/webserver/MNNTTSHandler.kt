package io.kindbrave.mnn.webserver.webserver

import android.content.Context
import com.elvishew.xlog.XLog
import dagger.hilt.android.qualifiers.ApplicationContext
import io.kindbrave.mnn.server.service.LLMService
import io.kindbrave.mnn.webserver.webserver.request.TTSTextRequest
import io.kindbrave.mnn.webserver.webserver.utils.TTSUtils
import kotlinx.io.IOException
import java.security.InvalidParameterException
import java.util.Arrays
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MNNTTSHandler @Inject constructor(
    private val llmService: LLMService,
    @ApplicationContext private val context: Context
) {
    private val tag = MNNTTSHandler::class.java.simpleName

    fun generateAudio(body: TTSTextRequest): ByteArray {
        val modelId = body.model
        val text = body.text

        if (modelId.isEmpty() || text.isEmpty()) {
            throw InvalidParameterException("Model ID or text cannot be empty")
        }

        val ttsSession = llmService.getTTSSession(modelId)
            ?: throw InvalidParameterException("TTS session not found for model: $modelId")

        val audioData = try {
            TTSUtils.addWavHeader(ttsSession.process(text, 0))
        } catch (e: Exception) {
            XLog.tag(tag).e("TTS generation failed: $e")
            throw IOException("Failed to generate audio: $e")
        }

        XLog.tag(tag).d("TTS generation successful")
        return audioData
    }
}