package com.youcruit.atmosphere.stomp.api

import com.youcruit.atmosphere.stomp.protocol.StompFrame
import org.atmosphere.util.uri.UriTemplate

interface StompRequestFrame {
    val originalDestination: String
    val headers: Map<String, String>
    val id: String?
    val variables: Map<String, String>
}

internal class StompRequestFrameImpl(
    internal val stompFrame: StompFrame,
    private val template: UriTemplate
) : StompRequestFrame {
    override val headers: Map<String, String> = stompFrame.headers
    override val originalDestination = stompFrame.destination!!
    override val id = stompFrame.id
    override val variables by lazy {
        val map = mutableMapOf<String, String>()
        template.match(originalDestination, map)
        map.toMap()
    }
}