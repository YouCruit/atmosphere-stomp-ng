package com.youcruit.atmosphere.stomp

import org.atmosphere.cpr.Action
import org.atmosphere.cpr.AtmosphereConfig
import org.atmosphere.cpr.AtmosphereFramework
import org.atmosphere.cpr.AtmosphereInterceptorAdapter
import org.atmosphere.cpr.AtmosphereResource
import org.atmosphere.interceptor.InvokationOrder

class FrameInterceptor : AtmosphereInterceptorAdapter() {
    private lateinit var framework: AtmosphereFramework

    override fun inspect(r: AtmosphereResource): Action {
        val action = super.inspect(r)

        return action
    }

    override fun configure(config: AtmosphereConfig) {
        framework = config.framework()
    }

    override fun priority() = InvokationOrder.PRIORITY.AFTER_DEFAULT
}