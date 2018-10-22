package com.youcruit.atmosphere.stomp

import com.youcruit.atmosphere.stomp.protocol.StompFrame
import org.atmosphere.config.managed.Decoder
import org.atmosphere.cpr.AtmosphereResource
import org.atmosphere.cpr.Broadcaster
import java.lang.reflect.Method

private typealias ParamProvider = (AtmosphereResource, StompFrame) -> Any

class InjectingMethodInvocation(
    private val method: Method,
    private val bodyConverter: Decoder<String, Any>,
    private val bodyParameterIndex: Int,
    private val broadcaster: Broadcaster
) {
    val paramProvider: Map<Class<*>, ParamProvider> =
        mapOf(
            AtmosphereResource::class.java to { atmosphereResource, _ ->
                atmosphereResource
            },
            Broadcaster::class.java to { atmosphereResource, _ ->
                broadcaster
            },
            StompFrame::class.java to { _, stompFrame ->
                stompFrame
            },
            Any::class.java to { _, stompFrame ->
                bodyConverter.decode(stompFrame.bodyAsString())
            }
        )
}