package com.youcruit.atmosphere.stomp.protocol

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

abstract class StompParser {
    fun parse(input: InputStream): StompFrame {
        val command = input.readCommand()
        val headers = input.readHeaderLines()
        val contentLength = headers["content-length"]?.toInt()
        val body = input.readBody(contentLength)
        return StompFrame(
            command = command,
            headers = headers,
            body = body
        )
    }

    open fun InputStream.readCommand() =
        ClientStompCommand.valueOf(readUtf8Line(50))

    internal open fun InputStream.readUtf8Line(atMost: Int = 2048): String {
        val baos = EfficientByteArrayOutputStream()
        for (i in 1..atMost) {
            val byte = read()
            if (byte == -1) {
                throw StompProtocolException("Unexpected end of stream")
            }
            if (byte == 0x10) {
                return baos.asString(StandardCharsets.UTF_8)
            }
            baos.write(byte)
        }
        throw IOException("data too long")
    }

    internal fun InputStream.readHeaderLines(): Map<String, String> {
        return generateSequence { readHeaderLine() }
            .toMap(LinkedHashMap())
    }

    fun InputStream.readBody(contentLength: Int?): ByteArray {
        if (contentLength != null) {
            val bytes = ByteArray(contentLength)
            val bytesRead = read(bytes)
            if (bytesRead != contentLength) {
                throw StompProtocolException("Stream closed prematurely")
            }
            if (read() != -1) {
                throw StompProtocolException("Body must be terminated by null character")
            }
            return bytes
        } else {
            val baos = ByteArrayOutputStream()
            generateSequence { read() }
                .takeWhile { it != 0 }
                .forEach { baos.write(it) }
            return baos.toByteArray()
        }
    }

    fun InputStream.readHeaderLine(): Pair<String, String>? {
        val line = this.readUtf8Line()
        if (line == "") {
            return null
        }
        val key = line.substringBefore(':')
        val value = line
            .substringAfter(':')
            .replace(Regex("\\\\(.)")) { it ->
                when (it.groupValues[0]) {
                    "r" -> "\r"
                    "n" -> "\n"
                    "c" -> ":"
                    "\\" -> "\\"
                    else -> throw StompProtocolException("Not a valid replacement: ${it.groupValues[0]}")
                }
            }
        return key to value
    }

    open fun eol(bytes: ByteArray): Boolean {
            return when(bytes.size) {
                1 -> bytes[0] == '\n'.toByte()
                else -> false
            }
        }
}

internal class EfficientByteArrayOutputStream : ByteArrayOutputStream() {
    fun asString(charset: Charset = StandardCharsets.UTF_8): String {
        return String(buf, 0, count, charset)
    }
}

