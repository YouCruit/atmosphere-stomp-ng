package com.youcruit.atmosphere.stomp

/**
 * Destination is always a "resolved" String, i.e. never a template.
 * Possible value is
 * "/foo/965544df-4191-446d-b6c1-a2530af97f82/"
 * and NEVER
 * "/foo/{barId}/"
 */
private typealias Destination = String

private typealias Id = String

class Subscriptions {
    private val idToDest = LinkedHashMap<Id, Destination>()
    private val destToId = LinkedHashMap<Destination, Id>()

    fun getById(id: Id): Destination? {
        return idToDest[id]
    }

    fun addSubscription(id: Id, destination: Destination) {
        idToDest[id] = destination
    }

    fun removeById(id: Id) {
        idToDest.remove(id)
    }

    fun findAllByDestination(destination: String): List<Id> {
        return destToId
            .asSequence()
            .filter { it.key.startsWith(destination) }
            .map { it.value }
            .toList()
    }
}