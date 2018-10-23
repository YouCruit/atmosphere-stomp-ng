package com.youcruit.atmosphere.stomp.invoker

import com.youcruit.atmosphere.stomp.api.MessageDecoder
import com.youcruit.atmosphere.stomp.protocol.StompFrame
import org.atmosphere.cpr.AtmosphereResource
import org.atmosphere.util.uri.UriTemplate
import java.lang.reflect.Method

internal class InjectingMethodInvocation(
    val method: Method,
    private val obj: Any,
    private val bodyConverter: MessageDecoder<*>
) {
    private fun getParameters(atmosphereResource: AtmosphereResource, stompFrame: StompFrame): Array<Any?> {
        return method
            .parameterTypes
            .map { it ->
                @Suppress("IMPLICIT_CAST_TO_ANY", "UNCHECKED_CAST")
                when (it.name) {
                    AtmosphereResource::class.java.name -> atmosphereResource
                    StompFrame::class.java.name -> stompFrame
                    else -> bodyConverter.decode(stompFrame.body, it as Class<in Any?>)
                }
            }.toTypedArray()
    }

    fun invoke(atmosphereResource: AtmosphereResource, stompFrame: StompFrame): Any {
        val params = getParameters(atmosphereResource, stompFrame)
        return method.invoke(obj, *params)
    }
}