package com.youcruit.atmosphere.stomp.api

import com.youcruit.atmosphere.stomp.protocol.StompFrameFromClient
import com.youcruit.atmosphere.stomp.util.FixedUriTemplate

typealias Destination = String

interface StompRequestFrame {
    val originalDestination: Destination
    val headers: Map<String, String>
    val id: String?
    val variables: Map<String, String>
}

internal class StompRequestFrameImpl(
    @Suppress("CanBeParameter")
    // For debugging
    internal val stompFrame: StompFrameFromClient,
    private val template: FixedUriTemplate
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