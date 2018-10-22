package com.youcruit.atmosphere.stomp

import org.atmosphere.cpr.Action
import org.atmosphere.cpr.AtmosphereConfig
import org.atmosphere.cpr.AtmosphereFramework
import org.atmosphere.cpr.AtmosphereInterceptor
import org.atmosphere.cpr.AtmosphereInterceptorWriter
import org.atmosphere.cpr.AtmosphereResource
import org.atmosphere.interceptor.InvokationOrder

class FrameInterceptor : AtmosphereInterceptor, InvokationOrder {
    private lateinit var framework: AtmosphereFramework

    override fun postInspect(r: AtmosphereResource) {
    }

    override fun inspect(r: AtmosphereResource): Action {
        val res = r.response
        if (res.asyncIOWriter == null) {
            res.asyncIOWriter(AtmosphereInterceptorWriter())
        }
        return Action.CONTINUE
    }

    override fun configure(config: AtmosphereConfig) {
        framework = config.framework()
    }

    override fun destroy() {
    }

    override fun priority() = InvokationOrder.PRIORITY.AFTER_DEFAULT
}