// Created by ruoyi.sjd on 2025/2/11.
// Copyright (c) 2024 Alibaba Group Holding Limited All rights reserved.
package io.kindbrave.mnn.webserver.modelapi.source

import io.kindbrave.mnn.webserver.modelapi.source.ModelSources.Companion.get

class RepoConfig(@JvmField var modelScopePath: String, var huggingFacePath: String, @JvmField var modelId: String) {
    fun repositoryPath(): String {
        return if (get().remoteSourceType == ModelSources.ModelSourceType.HUGGING_FACE) huggingFacePath else modelScopePath
    }
}