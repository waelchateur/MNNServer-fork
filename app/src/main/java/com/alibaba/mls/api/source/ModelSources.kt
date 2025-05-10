// Created by ruoyi.sjd on 2025/2/11.
// Copyright (c) 2024 Alibaba Group Holding Limited All rights reserved.
package com.alibaba.mls.api.source

import com.alibaba.mls.api.ApplicationProvider
import io.kindbrave.mnnserver.repository.SettingsRepository
import kotlinx.coroutines.runBlocking

class ModelSources() {
    private val settingsRepository = SettingsRepository(ApplicationProvider.get())
    enum class ModelSourceType {
        MODEL_SCOPE,
        HUGGING_FACE,
        MODELERS,
        LOCAL
    }

    fun getDownloadProvider(): ModelSources.ModelSourceType {
        val result = runBlocking {
            settingsRepository.getDownloadProvider()
        }
        return when (result) {
            "HuggingFace" -> ModelSources.ModelSourceType.HUGGING_FACE
            "ModelScope" -> ModelSources.ModelSourceType.MODEL_SCOPE
            else -> ModelSources.ModelSourceType.MODELERS
        }
    }

    val remoteSourceType: ModelSourceType
        get() {
            return getDownloadProvider()
        }

    private object InstanceHolder {
        val instance: ModelSources = ModelSources()
    }

    val config: ModelSourceConfig
        get() {
            if (mockConfig == null) {
                mockConfig = ModelSourceConfig.createMockSourceConfig()
            }
            return mockConfig!!
        }

    companion object {
        private var mockConfig: ModelSourceConfig? = null

        @JvmStatic
        fun get(): ModelSources {
            return InstanceHolder.instance
        }
    }
}
