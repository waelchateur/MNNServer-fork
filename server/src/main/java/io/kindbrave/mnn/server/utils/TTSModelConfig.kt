package io.kindbrave.mnn.server.utils

import com.elvishew.xlog.XLog
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.annotations.SerializedName
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.io.File

data class TTSModelConfig(
    @SerializedName("model_type") val modelType: TTSModelType,
    @SerializedName("model_path") val modelPath: String,
    // For sherpa
    val lexicon: List<String> = emptyList(),
    @SerializedName("data_dir") val dataDir: String = "",
    @SerializedName("dict_dir") val dictDir: String = "",
    val tokens: String = "",
    val voices: String = "",
    // For bertvits
    @SerializedName("asset_folder") val assetFolder: String = "",
    @SerializedName("sample_rate") val sampleRate: Int = 44100,
    @SerializedName("cache_folder") val vocabFile: String = "",
) {
    companion object {
        private val tag = TTSModelConfig::class.java.simpleName

        fun loadConfig(filePath: String): TTSModelConfig? {
            return try {
                val gson = GsonBuilder()
                    .registerTypeAdapter(TTSModelType::class.java, TTSModelTypeAdapter())
                    .create()

                val configFile = File(filePath)
                return gson.fromJson(configFile.readText(), TTSModelConfig::class.java)
            } catch (e: Exception) {
                XLog.tag(tag).e("Failed to load config file: $filePath", e)
                null
            }
        }
    }
}

sealed interface TTSModelType {
    val typeName: String

    object SHERPA_VITS : TTSModelType { override val typeName = "sherpa-vits" }
    object SHERPA_Matcha : TTSModelType { override val typeName = "sherpa-matcha" }
    object SHERPA_Kokoro : TTSModelType { override val typeName = "sherpa-kokoro" }
    object BERT_VITS : TTSModelType { override val typeName = "bertvits" }

    companion object {
        fun fromString(value: String): TTSModelType = when (value.lowercase()) {
            SHERPA_VITS.typeName -> SHERPA_VITS
            SHERPA_Matcha.typeName -> SHERPA_Matcha
            SHERPA_Kokoro.typeName -> SHERPA_Kokoro
            BERT_VITS.typeName -> BERT_VITS
            else -> throw IllegalArgumentException("Unknown model type: $value")
        }
    }
}

class TTSModelTypeAdapter : TypeAdapter<TTSModelType>() {
    override fun write(out: JsonWriter, value: TTSModelType) {
        out.value(value.typeName)
    }

    override fun read(reader: JsonReader): TTSModelType {
        return TTSModelType.fromString(reader.nextString())
    }
}
