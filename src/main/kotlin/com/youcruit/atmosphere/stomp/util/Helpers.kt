package com.youcruit.atmosphere.stomp.util

import com.youcruit.atmosphere.stomp.Subscriptions
import com.youcruit.atmosphere.stomp.protocol.Stomp12Protocol
import com.youcruit.atmosphere.stomp.protocol.StompProtocol
import org.atmosphere.cpr.AtmosphereResourceSession

internal const val PROTOCOL_VERSION = "com.youcruit.stomp.protocol.version"

internal var AtmosphereResourceSession.protocol: StompProtocol
    get() = getAttribute(PROTOCOL_VERSION) as StompProtocol? ?: Stomp12Protocol
    set(value) {
        setAttribute(PROTOCOL_VERSION, value)
    }

private val SUBSCRIPTIONS = "com.youcruit.atmosphere.stomp.Subscriptions"

internal var AtmosphereResourceSession.subscriptions: Subscriptions
    get() {
        return getAttribute(SUBSCRIPTIONS) as Subscriptions?
            ?: let {
                val sub = Subscriptions()
                setAttribute(SUBSCRIPTIONS, sub)
                sub
            }
    }
    set(value) {
        setAttribute(SUBSCRIPTIONS, value)
    }