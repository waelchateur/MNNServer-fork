// Created by ruoyi.sjd on 2024/12/25.
// Copyright (c) 2024 Alibaba Group Holding Limited All rights reserved.
// Created by KindBrave on 2025/03/26.
package io.kindbrave.mnn.server.engine

import android.util.Log
import com.alibaba.mnnllm.android.model.ModelUtils
import com.google.gson.Gson
import io.kindbrave.mnn.server.engine.MNNLlm.AudioDataListener
import io.kindbrave.mnn.server.engine.MNNLlm.GenerateProgressListener
import io.kindbrave.mnn.server.utils.FileUtils
import io.kindbrave.mnn.base.utils.ModelConfig
import java.io.File

class ChatSession(
    override val modelId: String,
    override var sessionId: String,
    override val configPath: String,
    private val isDiffusion: Boolean = false,
    private val diffusionMemoryMode: Int = 0
): Session(modelId, sessionId, configPath) {
    private val tag = ChatSession::class.java.simpleName

    private var extraAssistantPrompt: String? = null
    var supportOmni: Boolean = false

    private var nativePtr: Long = 0

    @Volatile
    private var modelLoading = false

    @Volatile
    private var generating = false

    @Volatile
    private var releaseRequeted = false

    fun load() {
        modelLoading = true

        val extraConfig = ModelConfig.loadConfig(configPath, getModelSettingsFile())?.apply {
            if (io.kindbrave.mnn.server.utils.ModelUtils.isNeedConfigThinkMode(modelId)) {
                extraAssistantPrompt = if (this.thinkingMode == true) {
                    "<|im_start|>assistant\n%s<|im_end|>\n"
                } else {
                    "<|im_start|>assistant\n<think>\n</think>%s<|im_end|>\n"
                }
                this.assistantPromptTemplate = extraAssistantPrompt
            }
        }
        var rootCacheDir: String? = ""
        if (extraConfig?.mmap == true) {
            rootCacheDir = FileUtils.getMmapDir(modelId, configPath.contains("modelscope"))
            File(rootCacheDir).mkdirs()
        }
        val configMap = HashMap<String, Any>().apply {
            put("is_diffusion", isDiffusion)
            put("is_r1", ModelUtils.isR1Model(modelId))
            put("mmap_dir", rootCacheDir ?: "")
            put("diffusion_memory_mode", diffusionMemoryMode)
        }
        nativePtr = MNNLlm.initNative(
            configPath,
            if (extraConfig != null) {
                Gson().toJson(extraConfig)
            } else {
                "{}"
            },
            Gson().toJson(configMap)
        )
        modelLoading = false
        if (releaseRequeted) {
            release()
        }
    }

    val debugInfo: String
        get() = MNNLlm.getDebugInfoNative(nativePtr) + "\n"

    fun generate(
        history: List<Pair<String, String>>,
        progressListener: GenerateProgressListener
    ): HashMap<String, Any> {
        synchronized(this) {
            generating = true
            val result = MNNLlm.submitNative(nativePtr, history, progressListener)
            generating = false
            if (releaseRequeted) {
                release()
            }
            return result
        }
    }

    fun generateDiffusion(
        input: String,
        output: String,
        iterNum: Int,
        randomSeed: Int,
        progressListener: GenerateProgressListener
    ): HashMap<String, Any> {
        synchronized(this) {
            Log.d(tag, "MNN_DEBUG submit$input")
            generating = true
            val result = MNNLlm.submitDiffusionNative(
                nativePtr,
                input,
                output,
                iterNum,
                randomSeed,
                progressListener
            )
            generating = false
            if (releaseRequeted) {
                releaseInner()
            }
            return result
        }
    }

    fun reset() {
        synchronized(this) {
            MNNLlm.resetNative(nativePtr, isDiffusion)
        }
    }

    fun release() {
        synchronized(this) {
            Log.d(
                tag,
                "MNN_DEBUG release nativePtr: $nativePtr mGenerating: $generating"
            )
            if (!generating && !modelLoading) {
                releaseInner()
            } else {
                releaseRequeted = true
                while (generating || modelLoading) {
                    try {
                        (this as Object).wait()
                    } catch (e: InterruptedException) {
                        Thread.currentThread().interrupt()
                        Log.e(tag, "Thread interrupted while waiting for release", e)
                    }
                }
                releaseInner()
            }
        }
    }

    fun loadConfig(): ModelConfig? {
        if (isDiffusion) {
            return null
        }
        return ModelConfig.loadConfig(configPath, getModelSettingsFile())
    }

    fun getModelSettingsFile():String {
        return FileUtils.getModelConfigDir(modelId) + "/custom_config.json"
    }

    private fun releaseInner() {
        if (nativePtr != 0L) {
            MNNLlm.releaseNative(nativePtr, isDiffusion)
            nativePtr = 0
            (this as Object).notifyAll()
        }
    }

    fun clearMmapCache() {
        FileUtils.clearMmapCache(modelId)
    }

    fun setAudioDataListener(listener: AudioDataListener?) {
        synchronized(this) {
            if (nativePtr != 0L) {
                MNNLlm.setWavformCallbackNative(nativePtr, listener)
            } else {
                Log.e(tag, "nativePtr null")
            }
        }
    }

    fun updateMaxNewTokens(maxNewTokens: Int) {
        MNNLlm.updateMaxNewTokensNative(nativePtr, maxNewTokens)
    }

    fun updateSystemPrompt(systemPrompt: String) {
        MNNLlm.updateSystemPromptNative(nativePtr, systemPrompt)
    }

    fun updateAssistantPrompt(assistantPrompt: String) {
        extraAssistantPrompt = assistantPrompt
        MNNLlm.updateAssistantPromptNative(nativePtr, assistantPrompt)
    }

    protected fun finalize() {
        release()
    }
}