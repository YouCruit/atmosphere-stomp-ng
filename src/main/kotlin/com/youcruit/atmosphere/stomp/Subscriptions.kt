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


    fun getById(id: Id): Destination? {
        return idToDest[id]
    }

    fun addSubscription(id: Id, destination: Destination) {
        idToDest[id] = destination
    }

    fun removeById(id: Id) {
        idToDest.remove(id)
    }
}