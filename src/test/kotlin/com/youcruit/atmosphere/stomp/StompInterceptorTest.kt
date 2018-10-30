package com.youcruit.atmosphere.stomp

import com.youcruit.atmosphere.stomp.protocol.ClientStompCommand
import com.youcruit.atmosphere.stomp.protocol.StompFrameFromClient
import org.atmosphere.cpr.StompTest
import org.junit.Before
import org.junit.Test

class StompInterceptorTest : StompTest() {

    @Before
    fun setUp() {
        initAtmosphere(StompTestService::class.java)
    }

    @Test
    fun stompServiceHappy() {
        subscribeStatus()
        runMessage(
            StompFrameFromClient(
                command = ClientStompCommand.SEND,
                headers = mapOf("destination" to "/send/happy"),
                body = "No care"
            ),
            true,
            ".*FOOOOOOO.*"
        )
    }

    @Test
    fun stompServiceSad() {
        subscribeStatus()
        runMessage(
            StompFrameFromClient(
                command = ClientStompCommand.SEND,
                headers = mapOf("destination" to "/send/sad"),
                body = "No care"
            ),
            true,
            ".*Fail.*"
        )
    }
}