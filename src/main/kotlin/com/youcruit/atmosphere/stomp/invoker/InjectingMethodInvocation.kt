package com.youcruit.atmosphere.stomp.invoker

import com.youcruit.atmosphere.stomp.FrameInterceptor
import com.youcruit.atmosphere.stomp.api.MessageDecoder
import com.youcruit.atmosphere.stomp.api.StompRequestFrame
import com.youcruit.atmosphere.stomp.api.StompRequestFrameImpl
import com.youcruit.atmosphere.stomp.protocol.StompFrame
import com.youcruit.atmosphere.stomp.util.FixedUriTemplate
import org.atmosphere.cpr.AtmosphereResource
import java.lang.reflect.Method

internal class InjectingMethodInvocation(
    val method: Method,
    private val obj: Any,
    private val bodyConverter: MessageDecoder<*>
) {
    private fun getParameters(atmosphereResource: AtmosphereResource, stompFrame: StompFrame, stompRequestFrame: StompRequestFrame): Array<Any?> {
        return method
            .parameterTypes
            .map { it ->
                @Suppress("IMPLICIT_CAST_TO_ANY", "UNCHECKED_CAST")
                when (it.name) {
                    AtmosphereResource::class.java.name -> atmosphereResource
                    StompRequestFrame::class.java.name -> stompRequestFrame
                    else -> bodyConverter.decode(stompFrame.body, it as Class<in Any?>)
                }
            }.toTypedArray()
    }

    fun invoke(atmosphereResource: AtmosphereResource, stompFrame: StompFrame, template: FixedUriTemplate): Any? {
        val stompRequestFrame = StompRequestFrameImpl(stompFrame, template)
        atmosphereResource.request.setAttribute(FrameInterceptor.STOMP_REQUEST_FRAME, stompRequestFrame)

        val params = getParameters(atmosphereResource, stompFrame, stompRequestFrame)
        return method.invoke(obj, *params)
    }
}