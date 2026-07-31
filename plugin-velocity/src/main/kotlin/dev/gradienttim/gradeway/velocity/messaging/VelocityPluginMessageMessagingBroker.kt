/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.velocity.messaging

import arrow.core.Either
import arrow.core.raise.context.either
import arrow.core.raise.context.raise
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.PluginMessageEvent
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.ServerConnection
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier
import dev.gradienttim.gradeway.constants.MessagingConstants
import dev.gradienttim.gradeway.messaging.MessagingBroker
import java.util.concurrent.ConcurrentHashMap

/**
 * A [MessagingBroker] backed by vanilla Minecraft plugin messaging channels, acting as the relay
 * hub for backend servers running [dev.gradienttim.gradeway.bukkit.messaging.
 * BukkitPluginMessageMessagingBroker] (which can never talk to each other directly). Every plugin
 * message received from a backend server is both dispatched locally (so this proxy's own caches
 * stay in sync) and rebroadcast to every backend server, including the origin - safe because
 * [CommonMessagingManager][dev.gradienttim.gradeway.managers.CommonMessagingManager] already
 * filters out self-originated payloads by `serverId`.
 *
 * @property server The proxy server used to register the channel and reach backend servers.
 * @property plugin The plugin instance this broker's event listener is registered under.
 */
class VelocityPluginMessageMessagingBroker(
    private val server: ProxyServer,
    private val plugin: Any
) : MessagingBroker {
    private val listeners = ConcurrentHashMap<MinecraftChannelIdentifier, ((payload: ByteArray) -> Boolean)>()

    override val warnNoEncryption: Boolean = true

    private val syncIdentifier = MinecraftChannelIdentifier.from(MessagingConstants.SYNC_CHANNEL)

    override fun open(): Either<Throwable, Unit> = either {
        try {
            server.channelRegistrar.register(syncIdentifier)
            server.eventManager.register(plugin, this)
        } catch (throwable: Throwable) {
            raise(throwable)
        }
    }

    override fun close(): Either<Throwable, Unit> = either {
        try {
            server.eventManager.unregisterListener(plugin, this)
            server.channelRegistrar.unregister(syncIdentifier)
        } catch (throwable: Throwable) {
            raise(throwable)
        }
    }

    override fun publish(channel: String, payload: ByteArray): Boolean {
        return server.allServers.any { registeredServer ->
            registeredServer.sendPluginMessage(syncIdentifier, payload)
        }
    }

    override fun subscribe(channel: String, handler: (payload: ByteArray) -> Boolean): Boolean {
        if (channel == syncIdentifier.id) {
            listeners[syncIdentifier] = handler
        }
        return true
    }

    @Subscribe
    fun onPluginMessage(event: PluginMessageEvent) {
        if (event.identifier != syncIdentifier) return

        val source = event.source
        if (source !is ServerConnection) return

        event.result = PluginMessageEvent.ForwardResult.handled()

        val payload = event.data
        val isSuccess = listeners[event.identifier]?.invoke(payload) ?: return
        if (isSuccess) {
            publish(MessagingConstants.SYNC_CHANNEL, payload)
        }
    }
}
