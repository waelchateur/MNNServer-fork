// Created by ruoyi.sjd on 2025/2/28.
// Copyright (c) 2024 Alibaba Group Holding Limited All rights reserved.

// Modified by KindBrave on 2025/5/31.
package com.alibaba.mnnllm.android.mainsettings

import android.content.Context
import com.alibaba.mls.api.source.ModelSources
import com.alibaba.mnnllm.android.utils.DeviceUtils
import io.kindbrave.mnnserver.repository.SettingsRepository
import kotlinx.coroutines.runBlocking


object MainSettings {

    fun getDownloadProvider(context: Context): ModelSources.ModelSourceType {
        val result = getDownloadProviderString(context)
        return when (result) {
            "HuggingFace" -> ModelSources.ModelSourceType.HUGGING_FACE
            "ModelScope" -> ModelSources.ModelSourceType.MODEL_SCOPE
            else -> ModelSources.ModelSourceType.MODELERS
         }
    }

    fun getDownloadProviderString(context: Context):String {
        val settingsRepository = SettingsRepository(context)
        return  runBlocking {
            settingsRepository.getDownloadProvider()
        }
    }

//    fun getDefaultDownloadProvider():String {
//        return if (DeviceUtils.isChinese) "ModelScope" else "HuggingFace"
//    }
//
//    fun isStopDownloadOnChatEnabled(context: Context): Boolean {
//        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
//        return sharedPreferences.getBoolean("stop_download_on_chat", true)
//    }
//
//    fun getDiffusionMemoryMode(context: Context): String {
//        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
//        return sharedPreferences.getString("diffusion_memory_mode", DiffusionMemoryMode.MEMORY_MODE_SAVING.value)!!
//    }
//
//    fun setDiffusionMemoryMode(context: Context, mode:String) {
//        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
//        sharedPreferences.edit()
//            .putString("diffusion_memory_mode", mode)
//            .apply()
//    }

}