/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.driver

import dev.gradienttim.gradeway.driver.adapters.MessagingAdapter
import dev.gradienttim.gradeway.driver.meta.CreateDriver
import dev.gradienttim.gradeway.driver.meta.DriverType
import dev.gradienttim.gradeway.platform.Environment
import redis.clients.jedis.DefaultJedisClientConfig
import redis.clients.jedis.RedisClient

@CreateDriver(
    id = "redis",
    type = DriverType.MESSAGING
)
class RedisDriver : Driver(), MessagingAdapter {
    private var redisClient: RedisClient? = null

    override fun open(environment: Environment) {
        val messagingServerHost = environment.stringDefault(
            names = arrayOf("GRADEWAY_MESSAGING_HOST", "GRADEWAY_REDIS_HOST"),
            default = "localhost"
        )

        val messagingServerPort = environment.intDefault(
            names = arrayOf("GRADEWAY_MESSAGING_PORT", "GRADEWAY_REDIS_PORT"),
            default = 6379
        )

        val messagingUserName = environment.string(
            names = arrayOf("GRADEWAY_MESSAGING_USER", "GRADEWAY_REDIS_USER")
        )

        val messagingUserPassword = environment.string(
            names = arrayOf("GRADEWAY_MESSAGING_PASSWORD", "GRADEWAY_REDIS_PASSWORD")
        )

        val redisDatabase = environment.intDefault(
            names = arrayOf("GRADEWAY_REDIS_DATABASE"),
            default = 0
        )

        val redisConfigBuilder = DefaultJedisClientConfig.builder()
            .database(redisDatabase)

        messagingUserName?.let { redisConfigBuilder.user(it) }
        messagingUserPassword?.let { redisConfigBuilder.password(it) }

        redisClient = RedisClient.builder()
            .hostAndPort(messagingServerHost, messagingServerPort)
            .clientConfig(redisConfigBuilder.build())
            .build()
    }

    override fun close() {
        redisClient?.close()
    }
}
