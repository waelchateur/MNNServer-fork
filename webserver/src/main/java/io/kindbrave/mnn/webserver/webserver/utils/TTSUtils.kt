package io.kindbrave.mnn.webserver.webserver.utils

import java.nio.ByteBuffer
import java.nio.ByteOrder

object TTSUtils {
    fun shortArrayToWavFile(shorts: ShortArray, sampleRate: Int = 44100): ByteArray {
        val channelCount = 1

        val dataSize = shorts.size * 2
        val headerSize = 44
        val totalSize = headerSize + dataSize

        val buffer = ByteBuffer.allocate(totalSize)
        buffer.order(ByteOrder.LITTLE_ENDIAN)

        buffer.put("RIFF".toByteArray())
        buffer.putInt(totalSize - 8)
        buffer.put("WAVE".toByteArray())

        buffer.put("fmt ".toByteArray())
        buffer.putInt(16)
        buffer.putShort(1)
        buffer.putShort(channelCount.toShort())
        buffer.putInt(sampleRate)
        buffer.putInt(sampleRate * channelCount * 2)
        buffer.putShort((channelCount * 2).toShort())
        buffer.putShort(16)

        buffer.put("data".toByteArray())
        buffer.putInt(dataSize)

        for (s in shorts) {
            buffer.putShort(s)
        }

        return buffer.array()
    }
}