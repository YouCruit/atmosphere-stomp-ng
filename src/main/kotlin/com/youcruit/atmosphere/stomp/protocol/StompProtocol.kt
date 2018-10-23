package com.youcruit.atmosphere.stomp.protocol

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.Writer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.SortedSet

abstract class StompProtocol(
    open val version: Float
) {
    internal fun parse(input: InputStream): StompFrame {
        try {
            val command = input.readCommand()
            val headers = input.readHeaderLines()
            val contentLength = headers["content-length"]?.toInt()
            val body = input.readBody(contentLength)
            return StompFrame(
                command = command,
                headers = headers,
                body = body
            )
        } catch (e: IOException) {
            throw StompErrorException("Error reading stream", e)
        }
    }

    open fun InputStream.readCommand() =
        ClientStompCommand.valueOf(readUtf8Line(14))

    protected open fun InputStream.readUtf8Line(atMost: Int = 2048): String {
        val baos = EfficientByteArrayOutputStream()
        for (i in 1..atMost) {
            val byte = read()
            if (byte == -1) {
                throw StompErrorException("Unexpected end of stream")
            }
            if (byte == 0x0a) {
                return baos.asString(StandardCharsets.UTF_8)
            }
            baos.write(byte)
        }
        throw StompErrorException("data too long")
    }

    private fun InputStream.readHeaderLines(): Map<String, String> {
        // From the spec:
        // If a client or a server receives repeated frame header entries,
        // only the first header entry SHOULD be used as the value of header entry.
        //
        // Subsequent values are only used to maintain a history of state changes of the header
        // and MAY be ignored.
        return generateSequence { readHeaderLine() }
            .groupBy { it.first }
            .mapValuesTo(LinkedHashMap()) { it.value[0].second }
    }

    private fun InputStream.readBody(contentLength: Int?): ByteArray {
        if (contentLength != null) {
            val bytes = ByteArray(contentLength)
            val bytesRead = read(bytes)
            if (bytesRead != contentLength) {
                throw StompErrorException("Stream closed prematurely")
            }
            val nextByte = read()
            if (nextByte != 0) {
                throw StompErrorException("Body must be terminated by null character")
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

    private fun InputStream.readHeaderLine(): Pair<String, String>? {
        val line = this.readUtf8Line()
        if (line == "") {
            return null
        }
        val key = line.substringBefore(':')
            .unescapeHeader()
        val value = line
            .substringAfter(':')
            .unescapeHeader()
        return key to value
    }

    private fun Writer.writeHeaderLine(key: String, value: String) {
        write(key.escapeHeader())
        write(':'.toInt())
        write(value.escapeHeader())
        write('\n'.toInt())
    }

    private fun Writer.writeEmptyHeaderLine() {
        write('\n'.toInt())
    }

    open fun eol(bytes: ByteArray): Boolean {
        return when (bytes.size) {
            1 -> bytes[0] == '\n'.toByte()
            else -> false
        }
    }

    internal fun encodeFrame(stompFrame: StompFrame): ByteArray {
        val baos = ByteArrayOutputStream()
        writeFrame(baos, stompFrame)
        return baos.toByteArray()
    }

    internal fun writeFrame(baos: ByteArrayOutputStream, stompFrame: StompFrame) {
        baos.writer(StandardCharsets.UTF_8).use {
            it.write(stompFrame.command.name)
            it.write('\n'.toInt())
            if (stompFrame.body.isNotEmpty()) {
                it.writeHeaderLine("content-length", stompFrame.body.size.toString())
            }
            stompFrame
                .headers
                .filter { (key) -> key != "content-length" }
                .forEach { (key, value) ->
                    it.writeHeaderLine(key, value)
                }
            it.writeEmptyHeaderLine()
        }
        baos.write(stompFrame.body)
        baos.write(0)
    }

    protected open fun String.unescapeHeader(): String {
        return replace(Regex("\\\\(.)")) { it ->
            when (it.groupValues[0]) {
                "r" -> "\r"
                "n" -> "\n"
                "c" -> ":"
                "\\" -> "\\"
                else -> throw StompErrorException("Not a valid replacement: ${it.groupValues[0]}")
            }
        }
    }

    protected open fun String.escapeHeader(): String {
        return replace(Regex("([\\\\\r\n:])")) { it ->
            when (it.groupValues[0]) {
                "\r" -> "\\r"
                "\n" -> "\\n"
                ":" -> "\\c"
                "\\" -> "\\\\"
                else -> throw StompErrorException("Not a valid replacement: ${it.groupValues[0]}")
            }
        }
    }
}

internal class EfficientByteArrayOutputStream : ByteArrayOutputStream() {
    fun asString(charset: Charset = StandardCharsets.UTF_8): String {
        return String(buf, 0, count, charset)
    }
}

val AVAILABLE_STOMP_PROTOCOLS =
    sequenceOf(
        Stomp10Protocol,
        Stomp11Protocol,
        Stomp12Protocol
    ).sortedByDescending { it.version }

internal fun selectBestProtocol(clientProtocols: SortedSet<Float>): StompProtocol? {
    for (stompParser in AVAILABLE_STOMP_PROTOCOLS) {
        if (clientProtocols.contains(stompParser.version)) {
            return stompParser
        }
    }
    return null
}