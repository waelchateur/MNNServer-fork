package io.kindbrave.mnn.server.utils

object TTSArrayUtils {
    fun shortArrayToByteArray(shorts: ShortArray): ByteArray {
        val bytes = ByteArray(shorts.size * 2)
        for (i in shorts.indices) {
            val short = shorts[i]
            bytes[i * 2] = (short.toInt() and 0xFF).toByte()
            bytes[i * 2 + 1] = (short.toInt() shr 8 and 0xFF).toByte()
        }
        return bytes
    }

    fun floatArrayToByteArray(samples: FloatArray): ByteArray {
        val pcmBytes = ByteArray(samples.size * 2) // 16-bit PCM = 2 bytes/sample
        for (i in samples.indices) {
            val s = (samples[i].coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
            pcmBytes[i * 2] = (s.toInt() and 0xFF).toByte()
            pcmBytes[i * 2 + 1] = ((s.toInt() shr 8) and 0xFF).toByte()
        }

        return pcmBytes
    }
}