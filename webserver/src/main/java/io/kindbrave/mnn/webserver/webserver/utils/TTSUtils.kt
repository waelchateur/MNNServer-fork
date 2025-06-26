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

    fun addWavHeader(data: ByteArray, sampleRate: Int = 44100, channels: Int = 1, bitsPerSample: Int = 16): ByteArray {
        val dataLength = data.size
        val header = createWavHeader(sampleRate, channels, bitsPerSample, dataLength)
        return header + data
    }

    private fun createWavHeader(sampleRate: Int, channels: Int, bitsPerSample: Int, dataLength: Int): ByteArray {
        val totalDataLen = 36 + dataLength
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8

        val header = ByteArray(44)
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = blockAlign.toByte()
        header[33] = 0
        header[34] = bitsPerSample.toByte()
        header[35] = 0
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (dataLength and 0xff).toByte()
        header[41] = ((dataLength shr 8) and 0xff).toByte()
        header[42] = ((dataLength shr 16) and 0xff).toByte()
        header[43] = ((dataLength shr 24) and 0xff).toByte()

        return header
    }
}