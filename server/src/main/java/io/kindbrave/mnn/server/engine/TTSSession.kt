// Created by ruoyi.sjd on 2024/12/25.
// Copyright (c) 2024 Alibaba Group Holding Limited All rights reserved.
// Created by KindBrave on 2025/03/26.
package io.kindbrave.mnn.server.engine

import android.util.Log
import com.k2fsa.sherpa.mnn.OfflineTtsConfig
import com.k2fsa.sherpa.mnn.OfflineTtsKokoroModelConfig
import com.k2fsa.sherpa.mnn.OfflineTtsMatchaModelConfig
import com.k2fsa.sherpa.mnn.OfflineTtsModelConfig
import com.k2fsa.sherpa.mnn.OfflineTtsVitsModelConfig
import com.taobao.meta.avatar.tts.TtsService
import io.kindbrave.mnn.server.utils.TTSArrayUtils
import io.kindbrave.mnn.server.utils.TTSModelConfig
import io.kindbrave.mnn.server.utils.TTSModelType
import io.kindbrave.mnn.sherpa.SherpaTts

class TTSSession(
    override val modelId: String,
    override var sessionId: String,
    override val configPath: String,
): Session(modelId, sessionId, configPath) {
    private val tag = TTSSession::class.java.simpleName

    private var modelType: TTSModelType = TTSModelType.BERT_VITS
    private val ttsService = TtsService()
    private val sherpaTts = SherpaTts()

    private var sampleRate = 44100

    suspend fun load() {
        val config = TTSModelConfig.loadConfig("$configPath/config.json")
        if (config == null) {
            throw Exception("Failed to load config file: $configPath")
        }
        modelType = config.modelType
        sampleRate = config.sampleRate

        when (modelType) {
            TTSModelType.BERT_VITS -> {
                ttsService.init(configPath)
            }
            TTSModelType.SHERPA_VITS -> {
                val modelConfig = OfflineTtsModelConfig(
                    vits = OfflineTtsVitsModelConfig(
                        model = "${configPath}/${config.modelPath}",
                        tokens = "${configPath}/${config.tokens}",
                        dictDir = "${configPath}/${config.dictDir}",
                        lexicon = config.lexicon.joinToString(",") { "${configPath}/${it}" },
                    ),
                    numThreads = 4,
                    debug = true,
                )
                sherpaTts.init(OfflineTtsConfig(modelConfig))
            }
            TTSModelType.SHERPA_Kokoro -> {
                val modelConfig = OfflineTtsModelConfig(
                    kokoro = OfflineTtsKokoroModelConfig(
                        model = "${configPath}/${config.modelPath}",
                        voices = "${configPath}/${config.voices}",
                        tokens = "${configPath}/${config.tokens}",
                        dataDir = "${configPath}/${config.dataDir}",
                        dictDir = "${configPath}/${config.dictDir}",
                        lexicon = config.lexicon.joinToString(",") { "${configPath}/${it}" },
                    ),
                    numThreads = 4,
                    debug = true,
                )
                sherpaTts.init(OfflineTtsConfig(modelConfig))
            }
            TTSModelType.SHERPA_Matcha -> {
                val modelConfig = OfflineTtsModelConfig(
                    matcha = OfflineTtsMatchaModelConfig(
                        acousticModel = "${configPath}/${config.modelPath}",
                        tokens = "${configPath}/${config.tokens}",
                        dataDir = "${configPath}/${config.dataDir}",
                        dictDir = "${configPath}/${config.dictDir}",
                        lexicon = config.lexicon.joinToString(",") { "${configPath}/${it}" },
                    ),
                    numThreads = 4,
                    debug = true,
                )
                sherpaTts.init(OfflineTtsConfig(modelConfig))
            }
        }
    }

    fun getSampleRate(): Int {
        return sampleRate
    }

    fun process(text: String, id: Int): ByteArray {
        return when (modelType) {
            TTSModelType.BERT_VITS -> {
                TTSArrayUtils.shortArrayToByteArray(ttsService.process(text, id))
            }

            TTSModelType.SHERPA_VITS,
            TTSModelType.SHERPA_Kokoro,
            TTSModelType.SHERPA_Matcha -> {
                val audio = sherpaTts.process(text)
                Log.d(tag, "Processed text: $text, audio: ${audio?.sampleRate}")
                if (audio == null) {
                    throw Exception("Failed to process text: $text")
                }
                TTSArrayUtils.floatArrayToByteArray(audio.samples)
            }
        }
    }

    fun release() {
        ttsService.destroy()
    }

    protected fun finalize() {
        release()
    }
}