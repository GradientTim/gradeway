/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.bungee.messaging

import arrow.core.Either
import arrow.core.raise.either
import dev.gradienttim.gradeway.bungee.GradewayPlugin
import dev.gradienttim.gradeway.constants.MessagingConstants
import dev.gradienttim.gradeway.messaging.MessagingBroker
import net.md_5.bungee.api.connection.Server
import net.md_5.bungee.api.event.PluginMessageEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.event.EventHandler
import java.util.concurrent.ConcurrentHashMap

/**
 * A [MessagingBroker] backed by vanilla Minecraft plugin messaging channels, acting as the relay
 * hub for backend servers running [dev.gradienttim.gradeway.bukkit.messaging.
 * PluginMessageMessagingBroker] (which can never talk to each other directly). Every plugin
 * message received from a backend server is both dispatched locally (so this proxy's own caches
 * stay in sync) and rebroadcast to every backend server, including the origin - safe because
 * [CommonMessagingManager][dev.gradienttim.gradeway.managers.CommonMessagingManager] already
 * filters out self-originated payloads by `serverId`.
 *
 * @property plugin The plugin used to register the channel, reach backend servers, and register
 * this broker's event listener under.
 */
class PluginMessageMessagingBroker(
    private val plugin: GradewayPlugin
) : MessagingBroker, Listener {
    private val listeners = ConcurrentHashMap<String, ((payload: ByteArray) -> Boolean)>()

    override val warnNoEncryption: Boolean = true

    override fun open(): Either<Throwable, Unit> = either {
        try {
            plugin.proxy.registerChannel(MessagingConstants.SYNC_CHANNEL)
            plugin.proxy.pluginManager.registerListener(plugin, this@PluginMessageMessagingBroker)
        } catch (throwable: Throwable) {
            raise(throwable)
        }
    }

    override fun close(): Either<Throwable, Unit> = either {
        try {
            plugin.proxy.pluginManager.unregisterListener(this@PluginMessageMessagingBroker)
            plugin.proxy.unregisterChannel(MessagingConstants.SYNC_CHANNEL)
        } catch (throwable: Throwable) {
            raise(throwable)
        }
    }

    override fun publish(channel: String, payload: ByteArray): Boolean {
        return plugin.proxy.servers.values.any { server ->
            server.sendData(channel, payload, true)
        }
    }

    override fun subscribe(channel: String, handler: (payload: ByteArray) -> Boolean): Boolean {
        listeners[channel] = handler
        return true
    }

    @EventHandler
    fun onPluginMessage(event: PluginMessageEvent) {
        if (event.sender !is Server) return

        val channel = event.tag
        if (channel != MessagingConstants.SYNC_CHANNEL) return

        event.isCancelled = true

        val isSuccess = listeners[channel]?.invoke(event.data) ?: return
        if (isSuccess) {
            publish(MessagingConstants.SYNC_CHANNEL, event.data)
        }
    }
}
