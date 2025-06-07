package io.kindbrave.mnnserver.utils

import java.util.Locale

object ModelUtils {
    private fun isQwen3Model(modelId: String): Boolean {
        return modelId.lowercase(Locale.getDefault()).contains("qwen3")
    }

    fun isNeedConfigThinkMode(modelId: String): Boolean {
        return isQwen3Model(modelId)
    }
}