package com.youcruit.atmosphere.stomp.protocol

class StompException : StompErrorException {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
}