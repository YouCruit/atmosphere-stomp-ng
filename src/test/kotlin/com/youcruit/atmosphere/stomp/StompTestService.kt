package com.youcruit.atmosphere.stomp

import com.youcruit.atmosphere.stomp.api.StompRequestFrame
import com.youcruit.atmosphere.stomp.api.annotations.StompEndpoint
import com.youcruit.atmosphere.stomp.api.annotations.StompService
import com.youcruit.atmosphere.stomp.api.annotations.StompSubscriptionService
import com.youcruit.atmosphere.stomp.api.exceptions.StompWithReplyException
import org.atmosphere.cpr.AtmosphereFramework
import org.atmosphere.cpr.Broadcaster
import java.util.UUID
import javax.inject.Inject
import kotlin.test.assertEquals

@StompEndpoint
class StompTestService {
    @Inject
    lateinit var atmosphereFramework: AtmosphereFramework

    @StompService("/send/sad")
    fun sendSad() {
        throw StompWithReplyException("Fail", "/status")
    }

    @StompService("/send/happy")
    fun sendHappy() {
        atmosphereFramework
            .atmosphereConfig
            .broadcasterFactory
            .lookup<Broadcaster>("/status", true)
            .broadcast("FOOOOOOOOOOOOOOOOOOOO")
    }

    @StompSubscriptionService("/listen/happy")
    fun happy(): Boolean {
        return true
    }

    @StompSubscriptionService("/listen/sad")
    fun sad(): Boolean {
        return false
    }

    @StompSubscriptionService("/listen/conditional/{shouldWork}")
    fun conditional(stompRequestFrame: StompRequestFrame): Boolean {
        return stompRequestFrame.variables["shouldWork"]!!.toBoolean()
    }

    @StompService("/send/foo/{testVar}")
    fun barbar(stompRequestFrame: StompRequestFrame) {
        assertEquals(HARDCODED_STRING_1, stompRequestFrame.variables["testVar"])
    }

    @StompSubscriptionService("/status")
    fun status() = true

    companion object {
        val HARDCODED_STRING_1 = UUID.randomUUID().toString()
    }
}