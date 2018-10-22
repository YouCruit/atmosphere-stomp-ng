package com.youcruit.atmosphere.stomp.protocol

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
            if (byte == 0x10) {
                return baos.asString(StandardCharsets.UTF_8)
            }
            if (byte == 0x13) {
                if (read() != 0x10) {
                    throw StompErrorException("CR must be escaped or be just before LF")
                }
                return baos.asString(StandardCharsets.UTF_8)
            }
        }
        throw StompErrorException("data too long")
    }

    override fun eol(bytes: ByteArray): Boolean {
            return when(bytes.size) {
                1 -> bytes[0] == '\n'.toByte()
                2 -> bytes[1] == '\r'.toByte() && bytes[0] == '\n'.toByte()
                else -> false
            }
        }
}



