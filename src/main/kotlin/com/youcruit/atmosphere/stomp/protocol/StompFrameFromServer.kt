package com.youcruit.atmosphere.stomp.protocol

class StompFrameFromServer(
    override val command: ServerStompCommand,
    headers: Map<String, String>,
    body: ByteArray
) : StompFrame(
    headers,
    body
) {
    constructor(
        command: ServerStompCommand,
        headers: Map<String, String>,
        body: String
    ) : this(
        command = command,
        headers = headers,
        body = body.toByteArray(getCharset(headers))
    )

    companion object {
        internal fun receiptOf(frame: StompFrameFromClient) =
            StompFrameFromServer(
                command = ServerStompCommand.RECEIPT,
                headers = mapOf("receipt-id" to frame.receipt!!),
                body = byteArrayOf()
            )
    }
}

enum class ServerStompCommand : StompCommand {
    CONNECTED,
    MESSAGE,
    RECEIPT,

    ERROR
}