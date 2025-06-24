// Created by ruoyi.sjd on 2024/12/26.
// Copyright (c) 2024 Alibaba Group Holding Limited All rights reserved.
// Created by KindBrave on 2025/03/26.
package io.kindbrave.mnn.mnnui.model

import java.io.Serializable

data class ChatDataItem(
    val role: String,
    val text: String
) : Serializable {
    companion object {
        const val ROLE_USER = "user"
        const val ROLE_ASSISTANT = "assistant"
        const val ROLE_SYSTEM = "system"
    }
}