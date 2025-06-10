package io.kindbrave.mnnserver.utils

import com.alibaba.mls.api.ModelItem

object ModelUtils {
    fun getModelTag(modelItem: ModelItem): String {
        return when {
            modelItem.getTags().contains("embedding") -> "embedding"
            modelItem.getTags().contains("asr") -> "asr"
            modelItem.getTags().contains("tts") -> "tts"
            else -> "chat"
        }
    }
}