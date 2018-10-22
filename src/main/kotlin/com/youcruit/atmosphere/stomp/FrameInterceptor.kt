package com.youcruit.atmosphere.stomp

import org.atmosphere.cpr.Action
import org.atmosphere.cpr.ApplicationConfig
import org.atmosphere.cpr.AtmosphereConfig
import org.atmosphere.cpr.AtmosphereFramework
import org.atmosphere.cpr.AtmosphereInterceptorAdapter
import org.atmosphere.cpr.AtmosphereResource
import org.atmosphere.cpr.AtmosphereResourceImpl
import org.atmosphere.interceptor.InvokationOrder
import org.atmosphere.util.IOUtils
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.UnsupportedEncodingException
import java.util.Arrays
import java.util.HashMap

class FrameInterceptor : AtmosphereInterceptorAdapter() {
    private lateinit var framework: AtmosphereFramework

    override fun inspect(r: AtmosphereResource): Action {
        val action = super.inspect(r)

        try {
            val body = IOUtils.readEntirelyAsString(r).toString()

            // Let the global handler suspend the connection if no action is submitted
            if (body.isEmpty()) {
                return Action.CONTINUE
            } else if (Arrays.equals(body.toByteArray(), ConnectInterceptor.STOMP_HEARTBEAT_DATA)) {

                // Particular case: the heartbeat is handled by the ConnectInterceptor
                val f = Frame(org.atmosphere.stomp.protocol.Action.NULL, HashMap())
                return inspect(framework, f, StompAtmosphereResource(r, f, handlerHelper))
            } else {
                val frame = stompFormat.parse(body.substring(0, body.length - 1))
                val sar = StompAtmosphereResource(r, frame, handlerHelper)

                try {
                    return inspect(framework, frame, sar)
                } finally {
                    sar.receipt()
                }
            }
        } catch (ioe: IOException) {
            logger.error("STOMP interceptor fails", ioe)
        } catch (pe: ParseException) {
            logger.error("Invalid STOMP string: {} ", body, pe)
        }

        return Action.CANCELLED
    }

    override fun configure(config: AtmosphereConfig) {
        framework = config.framework()
    }

    override fun priority() = InvokationOrder.PRIORITY.AFTER_DEFAULT

}