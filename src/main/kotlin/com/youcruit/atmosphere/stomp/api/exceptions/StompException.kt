package com.youcruit.atmosphere.stomp.api.exceptions

open class StompException : StompErrorException {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
}