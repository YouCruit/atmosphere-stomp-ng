package com.youcruit.atmosphere.stomp

import com.youcruit.atmosphere.stomp.invoker.StompHeartBeatInterceptor
import com.youcruit.atmosphere.stomp.invoker.stompHeartbeatInvoker
import com.youcruit.atmosphere.stomp.protocol.ClientStompCommand
import com.youcruit.atmosphere.stomp.protocol.StompFrame
import com.youcruit.atmosphere.stomp.protocol.eol
import org.atmosphere.cpr.Action
import org.atmosphere.cpr.ApplicationConfig
import org.atmosphere.cpr.AtmosphereConfig
import org.atmosphere.cpr.AtmosphereFramework
import org.atmosphere.cpr.AtmosphereInterceptorAdapter
import org.atmosphere.cpr.AtmosphereResource
import org.atmosphere.cpr.AtmosphereResourceEvent
import org.atmosphere.cpr.AtmosphereResourceEventListenerAdapter
import org.atmosphere.cpr.AtmosphereResourceImpl
import org.atmosphere.cpr.HeartbeatAtmosphereResourceEvent
import org.atmosphere.interceptor.InvokationOrder
import org.atmosphere.util.IOUtils
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.UnsupportedEncodingException
import java.text.ParseException
import java.util.Arrays
import java.util.HashMap

class FrameInterceptor : AtmosphereInterceptorAdapter() {
    private lateinit var framework: AtmosphereFramework
    private lateinit var heartBeatInterceptor: StompHeartBeatInterceptor


    override fun inspect(r: AtmosphereResource): Action {
        r.
        val action = super.inspect(r)

        try {
            val body = IOUtils.readEntirelyAsByte(r)

            // Let the global handler suspend the connection if no action is submitted
            if (body.isEmpty()) {
                return Action.CONTINUE
            } else if (body.eol) {
                val event = HeartbeatAtmosphereResourceEvent(AtmosphereResourceImpl::class.java.cast(r.resource))
                // Fire event
                r.notifyListeners(event)
                return Action.SKIP_ATMOSPHEREHANDLER
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
        heartBeatInterceptor = framework.atmosphereConfig.stompHeartbeatInvoker()
    }

    override fun priority() = InvokationOrder.PRIORITY.AFTER_DEFAULT

    companion object {
        private val logger = LoggerFactory.getLogger(FrameInterceptor::class.java)!!
    }

}