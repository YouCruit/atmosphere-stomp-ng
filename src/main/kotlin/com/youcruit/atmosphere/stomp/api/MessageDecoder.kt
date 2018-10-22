package com.youcruit.atmosphere.stomp.api

interface MessageDecoder<out T> {
    /**
     * Decode the specified object of type U into object of type T
     *
     * @param bytes the encoded data
     * @param clazz the class of T for deserialization
     * @return a new object of type T
     */
    fun decode(bytes: ByteArray, clazz: Class<in T>): T
}

object DefaultTranscoder : MessageDecoder<ByteArray> {
    override fun decode(bytes: ByteArray, clazz: Class<in ByteArray>) = bytes
}
