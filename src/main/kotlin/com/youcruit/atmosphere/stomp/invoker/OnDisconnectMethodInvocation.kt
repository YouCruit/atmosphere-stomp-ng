package com.youcruit.atmosphere.stomp.invoker

import org.atmosphere.cpr.AtmosphereResource
import org.atmosphere.cpr.AtmosphereResourceEvent
import java.lang.reflect.Method

internal class OnDisconnectMethodInvocation(
    private val method: Method,
    private val obj: Any
) {
    private fun getParameters(event: AtmosphereResourceEvent): Array<Any> {
        return method
            .parameterTypes
            .map {
                when (it.name) {
                    AtmosphereResource::class.java.name -> event.resource as Any
                    AtmosphereResourceEvent::class.java.name -> event
                    else -> throw IllegalArgumentException("Not a valid injection")
                }
            }.toTypedArray()
    }

    fun invoke(event: AtmosphereResourceEvent): Any {
        val params = getParameters(event)
        return method.invoke(obj, *params)
    }
}