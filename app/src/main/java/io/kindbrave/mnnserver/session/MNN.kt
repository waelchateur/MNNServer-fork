package io.kindbrave.mnnserver.session

import io.kindbrave.mnnserver.model.ChatDataItem
import io.kindbrave.mnnserver.session.ChatSession.GenerateProgressListener

class MNN {
    external fun initNative(
        rootCacheDir: String?,
        modelId: String?,
        configPath: String?,
        useTmpPath: Boolean,
        isDiffusion: Boolean,
        isR1: Boolean,
        backend: Boolean,
        sampler: String?
    ): Long

    external fun submitNative(
        instanceId: Long,
        history: List<ChatDataItem>,
        listener: GenerateProgressListener?
    ): HashMap<String?, Any?>?

    external fun submitDiffusionNative(
        instanceId: Long,
        input: String?,
        outputPath: String?,
        iterNum: Int,
        randomSeed: Int,
        listener: GenerateProgressListener?
    ): HashMap<String?, Any?>?

    external fun resetNative(instanceId: Long)

    external fun getDebugInfoNative(instanceId: Long): String?

    external fun releaseNative(instanceId: Long, isDiffusion: Boolean)

    external fun initEmbeddingNative(
        rootCacheDir: String?,
        configPath: String?,
        backend: Boolean
    ): Long

    external fun getTextEmbeddingNative(
        instanceId: Long,
        text: String
    ): FloatArray?

    external fun computeSimilarityNative(
        instanceId: Long,
        text1: String,
        text2: String
    ): Float

    external fun releaseEmbeddingNative(instanceId: Long)

    companion object {
        init {
            System.loadLibrary("llm")
            System.loadLibrary("MNN_CL")
        }
    }
}