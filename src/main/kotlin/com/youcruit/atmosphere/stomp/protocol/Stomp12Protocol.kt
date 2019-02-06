package com.youcruit.atmosphere.stomp.protocol

import com.youcruit.atmosphere.stomp.api.exceptions.StompErrorException
import java.io.InputStream
import java.nio.charset.StandardCharsets

object Stomp12Protocol : StompProtocol(1.2f) {
    override fun InputStream.readUtf8Line(atMost: Int): String {
        val baos = EfficientByteArrayOutputStream()
        for (i in 1..atMost) {
            val byte = read()
            if (byte == -1) {
                throw StompErrorException("Unexpected end of stream")
            }
            if (byte.toByte() == newline) {
                return baos.asString(StandardCharsets.UTF_8)
            }
            if (byte.toByte() == cr) {
                if (read().toByte() != newline) {
                    throw StompErrorException("CR must be escaped or be just before LF")
                }
                return baos.asString(StandardCharsets.UTF_8)
            }
            baos.write(byte)
        }
        throw StompErrorException("data too long: ${baos.asString()}")
    }

    override fun eol(bytes: ByteArray): Boolean {
        return when (bytes.size) {
            1 -> bytes[0] == newline
            2 -> bytes[0] == cr && bytes[1] == newline
            else -> false
        }
    }

    private const val newline = '\n'.toByte()
    private const val cr = '\r'.toByte()
}