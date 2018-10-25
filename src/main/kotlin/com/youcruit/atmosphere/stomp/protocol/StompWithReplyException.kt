package com.youcruit.atmosphere.stomp.protocol

class StompWithReplyException: StompException {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
}
