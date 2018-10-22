package com.youcruit.atmosphere.stomp.util

import com.youcruit.atmosphere.stomp.Subscriptions
import com.youcruit.atmosphere.stomp.protocol.StompProtocol
import org.atmosphere.cpr.AtmosphereResourceSession

private const val PROTOCOL_VERSION = "com.youcruit.stomp.protocol.version"

internal var AtmosphereResourceSession.protocol: StompProtocol
    get() = getAttribute(PROTOCOL_VERSION) as StompProtocol
    set(value) = setAttribute(PROTOCOL_VERSION, value) as Unit

private val SUBSCRIPTIONS = "com.youcruit.atmosphere.stomp.Subscriptions"

internal var AtmosphereResourceSession.subscriptions: Subscriptions
    get() = getAttribute(SUBSCRIPTIONS) as Subscriptions
    set(value) = setAttribute(SUBSCRIPTIONS, value) as Unit
