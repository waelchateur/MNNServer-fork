// Created by ruoyi.sjd on 2025/4/29.
// Copyright (c) 2024 Alibaba Group Holding Limited All rights reserved.

package io.kindbrave.mnnserver.utils

import android.util.Log
import com.alibaba.mls.api.ApplicationProvider
import com.alibaba.mnnllm.android.model.ModelUtils
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File
import com.google.gson.annotations.SerializedName

data class ModelConfig(
    @SerializedName("llm_model") var llmModel: String?,
    @SerializedName("llm_weight") var llmWeight: String?,
    @SerializedName("backend_type") var backendType: String?,
    @SerializedName("thread_num") var threadNum: Int?,
    @SerializedName("precision") var precision: String?,
    @SerializedName("memory") var memory: String?,
    @SerializedName("system_prompt") var systemPrompt: String?,
    @SerializedName("sampler_type") var samplerType: String?,
    @SerializedName("mixed_samplers") var mixedSamplers: MutableList<String>?,
    @SerializedName("temperature") var temperature: Float?,
    @SerializedName("topP") var topP: Float?,
    @SerializedName("topK") var topK: Int?,
    @SerializedName("minP") var minP: Float?,
    var tfsZ:Float?,
    var typical:Float?,
    var penalty:Float?,
    @SerializedName("n_gram")var nGram:Int?,
    @SerializedName("ngram_factor")var nGramFactor:Float?,
    @SerializedName("max_new_tokens")var maxNewTokens:Int?,
    @SerializedName("assistant_prompt_template")var assistantPromptTemplate:String?,
    @SerializedName("thinking_mode")var thinkingMode: Boolean?,
    @SerializedName("mmap")var mmap: Boolean?,
    ) {
    fun deepCopy(): ModelConfig {
        return ModelConfig(
            llmModel = this.llmModel,
            llmWeight = this.llmWeight,
            backendType = this.backendType,
            threadNum = this.threadNum,
            precision = this.precision,
            memory = this.memory,
            systemPrompt = this.systemPrompt,
            samplerType = this.samplerType,
            mixedSamplers = this.mixedSamplers?.toMutableList(),
            temperature = this.temperature,
            topP = this.topP,
            topK = this.topK,
            minP = this.minP,
            tfsZ = this.tfsZ,
            typical = this.typical,
            penalty = this.penalty,
            nGram = this.nGram,
            nGramFactor = this.nGramFactor,
            maxNewTokens = this.maxNewTokens,
            assistantPromptTemplate = this.assistantPromptTemplate,
            thinkingMode = this.thinkingMode,
            mmap = this.mmap
        )
    }

    companion object {

        const val TAG = "ModelConfig"

        fun loadConfig(filePath: String): ModelConfig? {
            return try {
                val file = File(filePath)
                val json = file.readText()
                Gson().fromJson(json, ModelConfig::class.java)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        fun loadConfig(originalFilePath: String, overrideFilePath: String): ModelConfig? {
            return try {
                val originalFile = File(originalFilePath)
                val originalJson = JsonParser.parseString(originalFile.readText()).asJsonObject

                val overrideFile = File(overrideFilePath)
                if (overrideFile.exists()) {
                    val overrideJson = JsonParser.parseString(overrideFile.readText()).asJsonObject
                    mergeJson(originalJson, overrideJson)
                }
                Gson().fromJson(originalJson, ModelConfig::class.java)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        private fun mergeJson(original: JsonObject, override: JsonObject) {
            for (key in override.keySet()) {
                original.add(key, override.get(key))
            }
        }

        fun toJson(): String {
            return Gson().toJson(this)
        }

        fun saveConfig(filePath: String, config: ModelConfig): Boolean {
            return try {
                Log.d(TAG, "file is : $filePath")
                val file = File(filePath)
                FileUtils.ensureParentDirectoriesExist(file)
                val gson = GsonBuilder().setPrettyPrinting().create()
                val jsonString = gson.toJson(config)
                file.writeText(jsonString)
                true
            } catch (e: Exception) {
                Log.e(TAG, "saveConfig error", e)
                false
            }
        }

        fun getExtraConfigFile(modelId: String):String {
            return getModelConfigDir(modelId) + "/custom_config.json"
        }

        fun getModelConfigDir(modelId: String): String {
            val rootCacheDir =
                ApplicationProvider.get().filesDir.toString() + "/configs/" + ModelUtils.safeModelId(
                    modelId
                )
            return rootCacheDir
        }
    }
}

