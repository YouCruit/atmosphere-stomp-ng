package com.youcruit.atmosphere.stomp.api.exceptions

import com.youcruit.atmosphere.stomp.protocol.StompFrameFromServer

@Suppress("unused")
// Api
class StompWithReplyException : StompException {
    constructor(stompFrame: StompFrameFromServer) : super(stompFrame.bodyAsString())
    constructor(stompFrame: StompFrameFromServer, cause: Throwable) : super(stompFrame.bodyAsString(), cause)
}
