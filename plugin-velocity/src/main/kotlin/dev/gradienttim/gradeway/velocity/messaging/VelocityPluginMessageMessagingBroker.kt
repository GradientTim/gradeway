/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.velocity.messaging

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.PluginMessageEvent
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.ServerConnection
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier
import dev.gradienttim.gradeway.constants.MessagingConstants
import dev.gradienttim.gradeway.messaging.MessagingBroker

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
    private val plugin: Any,
) : MessagingBroker {
    private val identifier = MinecraftChannelIdentifier.from(MessagingConstants.SYNC_CHANNEL)
    private var listener: ((payload: ByteArray) -> Unit)? = null

    override fun open() {
        server.channelRegistrar.register(identifier)
        server.eventManager.register(plugin, this)
    }

    override fun close() {
        server.eventManager.unregisterListener(plugin, this)
        server.channelRegistrar.unregister(identifier)
    }

    override fun publish(channel: String, payload: ByteArray): Boolean {
        var sent = false
        server.allServers.forEach { registeredServer ->
            sent = registeredServer.sendPluginMessage(identifier, payload) || sent
        }
        return sent
    }

    override fun subscribe(channel: String, listener: (payload: ByteArray) -> Unit): Boolean {
        this.listener = listener
        return true
    }

    @Subscribe
    fun onPluginMessage(event: PluginMessageEvent) {
        if (event.identifier != identifier) return

        val source = event.source
        if (source !is ServerConnection) return

        event.result = PluginMessageEvent.ForwardResult.handled()
        listener?.invoke(event.data)
        publish(MessagingConstants.SYNC_CHANNEL, event.data)
    }
}
