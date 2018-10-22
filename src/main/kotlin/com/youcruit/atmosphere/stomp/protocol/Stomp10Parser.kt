package com.youcruit.atmosphere.stomp.protocol

import java.io.InputStream

object Stomp10Parser : StompParser() {
    override fun InputStream.readCommand(): ClientStompCommand {
        val command = generateSequence { readUtf8Line(10) }
            .dropWhile { it.isBlank() }
            .first()
        return ClientStompCommand.valueOf(command)
    }
}