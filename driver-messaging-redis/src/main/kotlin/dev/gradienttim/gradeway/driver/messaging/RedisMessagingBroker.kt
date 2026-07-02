/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.driver.messaging

import dev.gradienttim.gradeway.messaging.MessagingBroker
import redis.clients.jedis.BinaryJedisPubSub
import redis.clients.jedis.RedisClient
import redis.clients.jedis.builders.StandaloneClientBuilder
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

class RedisMessagingBroker(val builder: StandaloneClientBuilder<RedisClient>) : MessagingBroker {
    private var redisClient: RedisClient? = null
    private val activePubSubs = ConcurrentHashMap<String, BinaryJedisPubSub>()

    override fun open() {
        redisClient = builder.build()
    }

    override fun close() {
        activePubSubs.values.forEach { it.unsubscribe() }
        activePubSubs.clear()

        redisClient?.close()
        redisClient = null
    }

    override fun publish(channel: String, payload: ByteArray): Boolean {
        val client = redisClient ?: return false

        return runCatching {
            client.publish(channelToBytes(channel), payload)
        }.isSuccess
    }

    override fun subscribe(channel: String, listener: (payload: ByteArray) -> Unit): Boolean {
        val client = redisClient ?: return false

        try {
            val channelBytes = channelToBytes(channel)

            val jedisPubSub = object : BinaryJedisPubSub() {
                override fun onMessage(ch: ByteArray, message: ByteArray) {
                    runCatching { listener(message) }
                }
            }

            activePubSubs[channel] = jedisPubSub

            thread(isDaemon = true) {
                try {
                    client.subscribe(jedisPubSub, channelBytes)
                } finally {
                    activePubSubs.remove(channel)
                }
            }

            return true
        } catch (_: Throwable) {
            return false
        }
    }

    private fun channelToBytes(channel: String): ByteArray =
        channel.toByteArray(StandardCharsets.UTF_8)
}
