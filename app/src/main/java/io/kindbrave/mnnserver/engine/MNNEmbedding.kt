package io.kindbrave.mnnserver.engine

object MNNEmbedding {
    external fun initNative(
        configPath: String?,
        mergedConfigStr: String?,
        configJsonStr: String?
    ): Long

    external fun updateMaxNewTokensNative(it: Long, maxNewTokens: Int)

    external fun embedding(llmPtr: Long, text: String): FloatArray

    external fun releaseNative(instanceId: Long)

    init {
        System.loadLibrary("mnnllmapp")
    }
}