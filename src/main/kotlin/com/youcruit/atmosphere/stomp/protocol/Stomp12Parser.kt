package com.youcruit.atmosphere.stomp.protocol

import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets

object Stomp12Parser : StompParser() {
    override fun InputStream.readUtf8Line(atMost: Int): String {
        val baos = EfficientByteArrayOutputStream()
        var wasCR = false
        for (i in 1..atMost) {
            val byte = read()
            if (byte == -1) {
                throw StompProtocolException("Unexpected end of stream")
            }
            if (byte == 0x10) {
                return baos.asString(StandardCharsets.UTF_8)
            }
            if (wasCR) {
                baos.write(0x13)
                wasCR = false
            }
            if (byte == 0x13) {
                wasCR = true
            } else {
                baos.write(byte)
            }
        }
        throw IOException("data too long")
    }

    override val ByteArray.eol: Boolean
        get() {
            return when(size) {
                1 -> this[0] == '\n'.toByte()
                2 -> this[1] == '\r'.toByte() && this[0] == '\n'.toByte()
                else -> false
            }
        }
}



