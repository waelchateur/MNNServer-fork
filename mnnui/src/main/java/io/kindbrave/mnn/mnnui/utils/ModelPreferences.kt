// Created by ruoyi.sjd on 2025/2/13.
// Copyright (c) 2024 Alibaba Group Holding Limited All rights reserved.
// Created by KindBrave on 2025/03/26.
package io.kindbrave.mnn.mnnui.utils

import android.content.Context
import androidx.core.content.edit
import com.alibaba.mnnllm.android.model.ModelUtils.safeModelId

object ModelPreferences {
    const val TAG: String = "ModelPreferences"

    const val KEY_USE_MMAP: String = "USE_MMAP"
    const val KEY_BACKEND: String = "BACKEND"
    const val KEY_SAMPLER: String = "SAMPLER"


    fun setBoolean(context: Context, modelId: String, key: String?, value: Boolean) {
        context.getSharedPreferences(safeModelId(modelId), Context.MODE_PRIVATE)
            .edit { putBoolean(key, value) }
    }

    fun setString(context: Context, modelId: String, key: String?, value: String?) {
        context.getSharedPreferences(safeModelId(modelId), Context.MODE_PRIVATE)
            .edit { putString(key, value) }
    }

    fun useMmap(context: Context, modelId: String): Boolean {
        return getBoolean(context, modelId, KEY_USE_MMAP, true)
    }

    fun getBoolean(
        context: Context,
        modelId: String,
        key: String?,
        defaultValue: Boolean
    ): Boolean {
        return context.getSharedPreferences(safeModelId(modelId), Context.MODE_PRIVATE)
            .getBoolean(key, defaultValue)
    }

    fun getString(context: Context, modelId: String, key: String?, defaultValue: String?): String? {
        return context.getSharedPreferences(safeModelId(modelId), Context.MODE_PRIVATE)
            .getString(key, defaultValue)
    }
}
