package com.youcruit.atmosphere.stomp

interface MessageDecoder<T : Any> {
    /**
     * Decode the specified object of type U into object of type T
     *
     * @param s the encoded data
     * @param clazz the class of T for deserialization
     * @return a new object of type T
     */
    fun decode(s: String, clazz: Class<out T>): T

}

object DefaultTranscoder : MessageDecoder<String> {
    override fun decode(s: String, clazz: Class<out String>) = s
}
