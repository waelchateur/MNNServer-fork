// Created by ruoyi.sjd on 2025/1/10.
// Copyright (c) 2024 Alibaba Group Holding Limited All rights reserved.
package io.kindbrave.mnn.mnnui.utils

import android.content.Context
import com.alibaba.mnnllm.android.model.ModelUtils

object FileUtils {
    fun getModelConfigDir(context: Context, modelId: String): String {
        val rootCacheDir =
            context.filesDir.toString() + "/configs/" + ModelUtils.safeModelId(
                modelId
            )
        return rootCacheDir
    }
}

