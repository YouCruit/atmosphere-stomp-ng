package com.youcruit.atmosphere.stomp

import com.youcruit.atmosphere.stomp.protocol.ClientStompCommand
import com.youcruit.atmosphere.stomp.protocol.ServerStompCommand
import com.youcruit.atmosphere.stomp.protocol.StompFrameFromClient
import org.atmosphere.cpr.StompTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

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

    @Test
    fun subscribeWithoutId() {
        runMessage(
            StompFrameFromClient(
                command = ClientStompCommand.SUBSCRIBE,
                headers = mapOf(
                    "destination" to "/listen/sad",
                    "receipt" to "12345"
                ),
                body = "No care"
            ),
            true
        ) {
            assertEquals(ServerStompCommand.ERROR, it.command)
            assertEquals("SUBSCRIBE must have an id", it.headers["message"])
            true
        }
    }

    @Test
    fun failingSubscribe() {
        runMessage(
            StompFrameFromClient(
                command = ClientStompCommand.SUBSCRIBE,
                headers = mapOf(
                    "destination" to "/listen/sad",
                    "receipt" to "12345",
                    "id" to "123456"
                ),
                body = "No care"
            ),
            assertNoMessage = true
        ) {true}
    }
}