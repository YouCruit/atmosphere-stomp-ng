package com.youcruit.atmosphere.stomp.protocol

import java.io.IOException
import java.io.InputStream

object Stomp12Parser : StompParser() {
    override fun InputStream.readLine(atMost: Int): String {
        val baos = EfficientByteArrayOutputStream()
        var wasCR = false
        for (i in 1..atMost) {
            val byte = read()
            if (byte == -1) {
                throw StompProtocolException("Unexpected end of stream")
            }
            if (byte == 0x10) {
                return baos.toString()
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
}



