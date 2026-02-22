package com.example.voicetodo.voice

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class VoiceStorage(private val context: Context) {
    fun createAudioFile(extension: String = "m4a"): File {
        val dir = File(context.filesDir, "voice")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val name = "voice_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.$extension"
        return File(dir, name)
    }

    fun savePcmAsWav(pcm: ByteArray, sampleRate: Int = 16_000, channels: Short = 1, bitsPerSample: Short = 16): String? {
        if (pcm.isEmpty()) return null
        return runCatching {
            val file = createAudioFile("wav")
            FileOutputStream(file).use { output ->
                output.write(buildWavHeader(pcm.size, sampleRate, channels, bitsPerSample))
                output.write(pcm)
            }
            file.absolutePath
        }.getOrNull()
    }

    private fun buildWavHeader(
        pcmDataLength: Int,
        sampleRate: Int,
        channels: Short,
        bitsPerSample: Short,
    ): ByteArray {
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = (channels * bitsPerSample / 8).toShort()
        val totalDataLen = pcmDataLength + 36

        return ByteArray(44).apply {
            // RIFF header
            this[0] = 'R'.code.toByte()
            this[1] = 'I'.code.toByte()
            this[2] = 'F'.code.toByte()
            this[3] = 'F'.code.toByte()
            writeIntLE(4, totalDataLen)
            this[8] = 'W'.code.toByte()
            this[9] = 'A'.code.toByte()
            this[10] = 'V'.code.toByte()
            this[11] = 'E'.code.toByte()
            // fmt chunk
            this[12] = 'f'.code.toByte()
            this[13] = 'm'.code.toByte()
            this[14] = 't'.code.toByte()
            this[15] = ' '.code.toByte()
            writeIntLE(16, 16)
            writeShortLE(20, 1) // PCM format
            writeShortLE(22, channels.toInt())
            writeIntLE(24, sampleRate)
            writeIntLE(28, byteRate)
            writeShortLE(32, blockAlign.toInt())
            writeShortLE(34, bitsPerSample.toInt())
            // data chunk
            this[36] = 'd'.code.toByte()
            this[37] = 'a'.code.toByte()
            this[38] = 't'.code.toByte()
            this[39] = 'a'.code.toByte()
            writeIntLE(40, pcmDataLength)
        }
    }

    private fun ByteArray.writeIntLE(offset: Int, value: Int) {
        this[offset] = (value and 0xff).toByte()
        this[offset + 1] = ((value shr 8) and 0xff).toByte()
        this[offset + 2] = ((value shr 16) and 0xff).toByte()
        this[offset + 3] = ((value shr 24) and 0xff).toByte()
    }

    private fun ByteArray.writeShortLE(offset: Int, value: Int) {
        this[offset] = (value and 0xff).toByte()
        this[offset + 1] = ((value shr 8) and 0xff).toByte()
    }
}
