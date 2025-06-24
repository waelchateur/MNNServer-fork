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

    override fun onIsLanguageAvailable(
        lang: String,
        country: String,
        variant: String
    ): Int {
        return TextToSpeech.LANG_AVAILABLE
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

        // 按照标点符合分割分别，并保留标点符号
        val sentences = Regex("[^\\p{P}\\p{S}]+[\\p{P}\\p{S}]")
            .findAll(text)
            .map { it.value.trim() }
            .toList()

        val session = llmService.getTTSSession("KindBrave/bert-vits2-MNN-Custom")
        if (session == null) {
            callback.error()
            return
        }

        callback.start(41200, AudioFormat.ENCODING_PCM_16BIT, 1)
        val maxBufferSize = callback.maxBufferSize

        try {
            runBlocking {
                var nextSegmentJob: Deferred<ShortArray>? = null

                sentences.forEachIndexed { index, sentence ->
                    Log.d(tag, "onSynthesizeText: $sentence")
                    val currentSegmentData = if (nextSegmentJob != null) {
                        shortArrayToByteArray(nextSegmentJob.await())
                    } else {
                        shortArrayToByteArray(session.process(sentence, index))
                    }

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

    private fun shortArrayToByteArray(shorts: ShortArray): ByteArray {
        val bytes = ByteArray(shorts.size * 2)
        for (i in shorts.indices) {
            val short = shorts[i]
            bytes[i * 2] = (short.toInt() and 0xFF).toByte()
            bytes[i * 2 + 1] = (short.toInt() shr 8 and 0xFF).toByte()
        }
        return bytes
    }
}