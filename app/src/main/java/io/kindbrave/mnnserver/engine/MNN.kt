package io.kindbrave.mnnserver.engine

object MNN {
    external fun initNative(
        configPath: String?,
        mergedConfigStr: String?,
        configJsonStr: String?
    ): Long

    external fun submitNative(
        instanceId: Long,
        history: List<Pair<String, String>>,
        listener: GenerateProgressListener
    ): HashMap<String, Any>

    external fun submitDiffusionNative(
        instanceId: Long,
        input: String,
        outputPath: String,
        iterNum: Int,
        randomSeed: Int,
        progressListener: GenerateProgressListener
    ): HashMap<String, Any>

    external fun resetNative(instanceId: Long, isDiffusion: Boolean)

    external fun getDebugInfoNative(instanceId: Long): String

    external fun releaseNative(instanceId: Long, isDiffusion: Boolean)

    external fun setWavformCallbackNative(
        instanceId: Long,
        listener: AudioDataListener?
    ): Boolean

    external fun updateMaxNewTokensNative(it: Long, maxNewTokens: Int)

    external fun updateSystemPromptNative(llmPtr: Long, systemPrompt: String)

    external fun updateAssistantPromptNative(llmPtr: Long, assistantPrompt: String)

    interface GenerateProgressListener {
        fun onProgress(progress: String?): Boolean
    }

    interface AudioDataListener {
        fun onAudioData(data: FloatArray, isEnd: Boolean): Boolean
    }

    init {
        System.loadLibrary("mnnllmapp")
    }
}