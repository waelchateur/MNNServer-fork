// Created by ruoyi.sjd on 2025/1/10.
// Copyright (c) 2024 Alibaba Group Holding Limited All rights reserved.
package io.kindbrave.mnnserver.utils

import android.content.Context

object FileUtils {
    fun getModelConfigDir(context: Context, modelId: String): String {
        val rootCacheDir =
            context.filesDir.toString() + "/configs/" + com.alibaba.mnnllm.android.model.ModelUtils.safeModelId(
                modelId
            )
        return rootCacheDir
    }
}

