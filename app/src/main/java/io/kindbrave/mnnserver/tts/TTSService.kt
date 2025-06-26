package io.kindbrave.mnnserver.tts

import android.media.AudioFormat
import android.speech.tts.SynthesisCallback
import android.speech.tts.SynthesisRequest
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeechService
import android.util.Log
import com.elvishew.xlog.XLog
import dagger.hilt.android.AndroidEntryPoint
import io.kindbrave.mnn.mnnui.di.ApplicationScope
import io.kindbrave.mnn.mnnui.repository.SettingsRepository
import io.kindbrave.mnn.server.service.LLMService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class TTSService : TextToSpeechService() {

    private val tag = TTSService::class.java.simpleName

    @Inject
    lateinit var llmService: LLMService

    @Inject
    @ApplicationScope
    lateinit var scope: CoroutineScope

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private var modelId: String? = null

    override fun onIsLanguageAvailable(
        lang: String,
        country: String,
        variant: String
    ): Int {
        return runBlocking {
            modelId = settingsRepository.getDefaultTTSModelId()
            if (modelId.isNullOrBlank()) {
                XLog.tag(tag).e("Model id is null")
                TextToSpeech.ERROR
            }
            val session = llmService.getTTSSession(modelId!!)
            if (session == null) {
                XLog.tag(tag).e("Session is null")
                TextToSpeech.ERROR
            }

            TextToSpeech.LANG_AVAILABLE
        }
    }

    override fun onGetLanguage(): Array<out String> {
        return arrayOf(
            "en",
            "zh",
        )
    }

    override fun onLoadLanguage(
        lang: String,
        country: String,
        variant: String
    ): Int {
        return onIsLanguageAvailable(lang, country, variant)
    }

    override fun onStop() {

    }

    override fun onSynthesizeText(
        request: SynthesisRequest,
        callback: SynthesisCallback
    ) {
        val text = request.charSequenceText.toString()

        // 按照标点符合分割
//        val sentences = Regex("[^\\p{P}\\p{S}]+[\\p{P}\\p{S}]")
//            .findAll(text)
//            .map { it.value.trim() }
//            .toList()
        val sentences = text
            .split(Regex("[\\p{P}\\p{S}]+"))
            .map { it.replace(Regex("[^\\p{L}\\p{N}]"), "") }
            .filter { it.isNotBlank() }

        if (modelId.isNullOrBlank()) {
            callback.error()
            return
        }
        val session = llmService.getTTSSession(modelId!!)
        if (session == null) {
            callback.error()
            return
        }

        callback.start(session.getSampleRate(), AudioFormat.ENCODING_PCM_16BIT, 1)
        val maxBufferSize = callback.maxBufferSize

        try {
            runBlocking {
                var nextSegmentJob: Deferred<ByteArray>? = null

                sentences.forEachIndexed { index, sentence ->
                    Log.d(tag, "onSynthesizeText: $sentence")
                    val currentSegmentData =
                        nextSegmentJob?.await() ?: session.process(sentence, index)

                    nextSegmentJob = if (index < sentences.size - 1) {
                        scope.async(Dispatchers.Default) {
                            session.process(sentences[index + 1], index + 1)
                        }
                    } else {
                        null
                    }

                    var offset = 0
                    while (offset < currentSegmentData.size) {
                        val bytesToWrite = minOf(maxBufferSize, currentSegmentData.size - offset)
                        val result = callback.audioAvailable(currentSegmentData, offset, bytesToWrite)
                        if (result != TextToSpeech.SUCCESS) {
                            XLog.tag(tag).e("Error writing audio data")
                            callback.error()
                            return@runBlocking
                        }
                        offset += bytesToWrite
                    }
                }

                callback.done()
            }
        } catch (e: Exception) {
            XLog.tag(tag).e("onSynthesizeText error: ${e.message}", e)
            callback.error()
        }
    }
}