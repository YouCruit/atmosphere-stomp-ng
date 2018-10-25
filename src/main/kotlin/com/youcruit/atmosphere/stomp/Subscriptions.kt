package com.youcruit.atmosphere.stomp

import com.youcruit.atmosphere.stomp.api.Destination
import com.youcruit.atmosphere.stomp.protocol.EfficientByteArrayOutputStream
import com.youcruit.atmosphere.stomp.protocol.ServerStompCommand
import com.youcruit.atmosphere.stomp.protocol.StompFrameFromServer
import com.youcruit.atmosphere.stomp.protocol.StompProtocol
import java.nio.charset.StandardCharsets
import java.util.UUID

private typealias Id = String

/**
 * Destination is always a "resolved" String, i.e. never a template.
 * Possible value is
 * "/foo/965544df-4191-446d-b6c1-a2530af97f82/"
 * and NEVER
 * "/foo/{barId}/"
 */
internal class Subscriptions {
    private val idToDest = LinkedHashMap<Id, Destination>()

    fun getById(id: Id): Destination? {
        return idToDest[id]
    }

    fun addSubscription(id: Id, destination: Destination) {
        idToDest[id] = destination
    }

    fun removeById(id: Id) {
        idToDest.remove(id)
    }

    fun findAllByDestination(destination: Destination): List<Id> {
        return idToDest
            .asSequence()
            .filter { (_, dest) -> dest.startsWith(destination) }
            .map { it.key }
            .toList()
    }

    fun createFrames(destination: Destination, protocol: StompProtocol, message: Any, extraHeaders: Map<String, String> = mapOf()): EfficientByteArrayOutputStream {
        val ids = synchronized(this) {
            findAllByDestination(destination)
        }

        val baos = EfficientByteArrayOutputStream()

        val body = if (message is ByteArray) {
            message
        } else {
            message.toString().toByteArray(StandardCharsets.UTF_8)
        }

        // Generate a frame for each subscription
        for (id in ids) {
            protocol.writeFrame(
                baos,
                StompFrameFromServer(
                    command = ServerStompCommand.MESSAGE,
                    headers = extraHeaders + mapOf(
                        "subscription" to id,
                        "message-id" to UUID.randomUUID().toString(),
                        "destination" to destination
                    ),
                    body = body
                )
            )
        }
        return baos
    }
}