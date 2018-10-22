package com.youcruit.atmosphere.stomp

import com.youcruit.atmosphere.stomp.invoker.StompHeartBeatInterceptor
import com.youcruit.atmosphere.stomp.invoker.stompHeartbeatInvoker
import com.youcruit.atmosphere.stomp.protocol.ClientStompCommand
import com.youcruit.atmosphere.stomp.protocol.StompParser
import org.atmosphere.cpr.Action
import org.atmosphere.cpr.AtmosphereConfig
import org.atmosphere.cpr.AtmosphereFramework
import org.atmosphere.cpr.AtmosphereInterceptorAdapter
import org.atmosphere.cpr.AtmosphereResource
import org.atmosphere.cpr.AtmosphereResourceImpl
import org.atmosphere.cpr.AtmosphereResourceSession
import org.atmosphere.cpr.AtmosphereResourceSessionFactory
import org.atmosphere.cpr.HeartbeatAtmosphereResourceEvent
import org.atmosphere.interceptor.InvokationOrder
import org.atmosphere.util.IOUtils
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.IOException
import java.text.ParseException

class FrameInterceptor : AtmosphereInterceptorAdapter() {
    private lateinit var framework: AtmosphereFramework
    private lateinit var heartBeatInterceptor: StompHeartBeatInterceptor
    private lateinit var sessionFactory: AtmosphereResourceSessionFactory
    private val AtmosphereResource.resourceSession: AtmosphereResourceSession
        get() = sessionFactory.getSession(this)


    private var AtmosphereResource.protocol: StompParser
        get() = resourceSession.getAttribute(PROTOCOL_VERSION) as StompParser
        set(value) = resourceSession.setAttribute(PROTOCOL_VERSION, value) as Unit


    override fun inspect(r: AtmosphereResource): Action {
        framework.atmosphereConfig.sessionFactory().getSession(r).setAttribute("")
        val action = super.inspect(r)

        try {
            val body = IOUtils.readEntirelyAsByte(r)

            // Let the global handler suspend the connection if no action is submitted
            if (body.isEmpty()) {
                return Action.CONTINUE
            } else if (r.protocol.eol(body)) {
                val event = HeartbeatAtmosphereResourceEvent(r as AtmosphereResourceImpl)
                // Fire event
                r.notifyListeners(event)
                return Action.SKIP_ATMOSPHEREHANDLER
            } else {
                val frame = r.protocol.parse(ByteArrayInputStream(body))
                when (frame.command as ClientStompCommand) {
                    ClientStompCommand.CONNECT,
                    ClientStompCommand.STOMP -> {
                        val version = parseVersion(frame.headers["accept-version"])
                    }
                }

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

    private fun parseVersion(s: String):  {

    }

    override fun configure(config: AtmosphereConfig) {
        framework = config.framework()
        heartBeatInterceptor = framework.atmosphereConfig.stompHeartbeatInvoker()
        sessionFactory = framework.atmosphereConfig.sessionFactory()
    }

    override fun priority() = InvokationOrder.PRIORITY.AFTER_DEFAULT

    companion object {
        private val logger = LoggerFactory.getLogger(FrameInterceptor::class.java)!!

        private const val PROTOCOL_VERSION = "com.youcruit.stomp.protocol.version"
    }

}