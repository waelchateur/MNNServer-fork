// Created by ruoyi.sjd on 2024/12/25.
// Copyright (c) 2024 Alibaba Group Holding Limited All rights reserved.
package io.kindbrave.mnnserver.utils

import com.alibaba.mls.api.ModelItem
import com.alibaba.mnnllm.android.model.ModelUtils

object CustomModelUtils {

    private val embeddingList: MutableSet<String> = HashSet()

    init {
        embeddingList.add("taobao-mnn/bge-large-zh-MNN") //embedding
        embeddingList.add("taobao-mnn/gte_sentence-embedding_multilingual-base-MNN") //embedding
    }

    fun processList(hfModelItems: List<ModelItem>): List<ModelItem> {
        val modelItems = ModelUtils.processList(hfModelItems)
        val embeddingItems: MutableList<ModelItem> = ArrayList()
        for (item in hfModelItems) {
            if (embeddingList.contains(item.modelId)) {
                item.addTag("embedding")
                embeddingItems.add(item)
            }
        }
        val result: MutableList<ModelItem> = mutableListOf()
        result.addAll(modelItems)
        result.addAll(embeddingItems)
        return result
    }
}
