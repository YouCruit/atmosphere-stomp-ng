package com.youcruit.atmosphere.stomp.invoker

import org.atmosphere.cpr.AtmosphereResource
import java.lang.reflect.Method

internal class OnDisconnectMethodInvocation(
    private val method: Method,
    private val obj: Any
) {
    private fun getParameters(atmosphereResource: AtmosphereResource): Array<Any> {
        return method
            .parameterTypes
            .map {
                when (it.name) {
                    AtmosphereResource::class.java.name -> atmosphereResource
                    else -> throw IllegalArgumentException("Not a valid injection")
                }
            }.toTypedArray()
    }

    fun invoke(atmosphereResource: AtmosphereResource) {
        val params = getParameters(atmosphereResource)
        method.invoke(obj, *params)
    }
}