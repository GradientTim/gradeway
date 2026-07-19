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
import dev.gradienttim.gradeway.messaging.MessagingAuthenticator
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
 * [MessagingBroker] authenticates every message on top of the [ServerConnection] origin check
 * already performed in [onPluginMessage] - the backend side of this channel cannot tell a relayed
 * message from one sent directly by a connecting player's client, so it relies on that same
 * shared secret.
 *
 * @property server The proxy server used to register the channel and reach backend servers.
 * @property plugin The plugin instance this broker's event listener is registered under.
 */
class VelocityPluginMessageMessagingBroker(
    private val server: ProxyServer,
    private val plugin: Any,
    messagingAuthenticator: MessagingAuthenticator,
) : MessagingBroker(messagingAuthenticator) {
    private val identifier = MinecraftChannelIdentifier.from(MessagingConstants.SYNC_CHANNEL)

    override fun open() {
        server.channelRegistrar.register(identifier)
        server.eventManager.register(plugin, this)
    }

    override fun close() {
        server.eventManager.unregisterListener(plugin, this)
        server.channelRegistrar.unregister(identifier)
    }

    override fun publishAuthenticated(channel: String, payload: ByteArray): Boolean {
        return server.allServers.any { registeredServer ->
            registeredServer.sendPluginMessage(identifier, payload)
        }
    }

    override fun subscribeChannel(channel: String): Boolean = true

    @Subscribe
    fun onPluginMessage(event: PluginMessageEvent) {
        if (event.identifier != identifier) return

        val source = event.source
        if (source !is ServerConnection) return

        event.result = PluginMessageEvent.ForwardResult.handled()

        val verifiedPayload = dispatch(event.data) ?: return
        publish(MessagingConstants.SYNC_CHANNEL, verifiedPayload)
    }
}
