package io.kindbrave.mnn.webserver.webserver.utils

import android.media.AudioFormat
import java.nio.ByteBuffer
import java.nio.ByteOrder

object TTSUtils {
    fun shortArrayToWavFile(shorts: ShortArray, sampleRate: Int = 44100): ByteArray {
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val channelCount = 1 // 单声道

        val dataSize = shorts.size * 2 // 每个short占2字节
        val headerSize = 44
        val totalSize = headerSize + dataSize

        val buffer = ByteBuffer.allocate(totalSize)
        buffer.order(ByteOrder.LITTLE_ENDIAN)

        // WAV文件头
        buffer.put("RIFF".toByteArray()) // ChunkID
        buffer.putInt(totalSize - 8) // ChunkSize
        buffer.put("WAVE".toByteArray()) // Format

        buffer.put("fmt ".toByteArray()) // Subchunk1ID
        buffer.putInt(16) // Subchunk1Size
        buffer.putShort(1) // AudioFormat (PCM)
        buffer.putShort(channelCount.toShort()) // NumChannels
        buffer.putInt(sampleRate) // SampleRate
        buffer.putInt(sampleRate * channelCount * 2) // ByteRate
        buffer.putShort((channelCount * 2).toShort()) // BlockAlign
        buffer.putShort(16) // BitsPerSample

        buffer.put("data".toByteArray()) // Subchunk2ID
        buffer.putInt(dataSize) // Subchunk2Size

        // 写入音频数据
        for (s in shorts) {
            buffer.putShort(s)
        }

        return buffer.array()
    }
}