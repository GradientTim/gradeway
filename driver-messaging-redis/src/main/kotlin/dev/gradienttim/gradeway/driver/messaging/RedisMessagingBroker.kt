/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.driver.messaging

import arrow.core.Either
import arrow.core.raise.context.either
import arrow.core.raise.context.raise
import dev.gradienttim.gradeway.messaging.MessagingBroker
import redis.clients.jedis.BinaryJedisPubSub
import redis.clients.jedis.RedisClient
import redis.clients.jedis.builders.StandaloneClientBuilder
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

class RedisMessagingBroker(
    val builder: StandaloneClientBuilder<RedisClient>
) : MessagingBroker {
    private var redisClient: RedisClient? = null
    private val activePubSubs = ConcurrentHashMap<String, BinaryJedisPubSub>()

    override val warnNoEncryption: Boolean = false
    override val warnNoSigning: Boolean = false

    override fun open(): Either<Throwable, Unit> = either {
        try {
            redisClient = builder.build()
        } catch (throwable: Throwable) {
            raise(throwable)
        }
    }

    override fun close(): Either<Throwable, Unit> = either {
        try {
            activePubSubs.values.forEach { it.unsubscribe() }
            activePubSubs.clear()

            redisClient?.close()
            redisClient = null
        } catch (throwable: Throwable) {
            raise(throwable)
        }
    }

    override fun publish(channel: String, payload: ByteArray): Boolean {
        val client = redisClient ?: return false

        return runCatching {
            client.publish(channelToBytes(channel), payload)
        }.isSuccess
    }

    override fun subscribe(channel: String, handler: (payload: ByteArray) -> Boolean): Boolean {
        val client = redisClient ?: return false

        try {
            if (activePubSubs.containsKey(channel)) {
                activePubSubs[channel]!!.unsubscribe()
                activePubSubs.remove(channel)
            }

            val channelBytes = channelToBytes(channel)

            val jedisPubSub = object : BinaryJedisPubSub() {
                override fun onMessage(ch: ByteArray, message: ByteArray) {
                    runCatching { handler(message) }
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
