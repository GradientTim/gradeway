/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.bungee.messaging

import dev.gradienttim.gradeway.bungee.GradewayPlugin
import dev.gradienttim.gradeway.constants.MessagingConstants
import dev.gradienttim.gradeway.messaging.MessagingAuthenticator
import dev.gradienttim.gradeway.messaging.MessagingBroker
import net.md_5.bungee.api.connection.Server
import net.md_5.bungee.api.event.PluginMessageEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.event.EventHandler

/**
 * A [MessagingBroker] backed by vanilla Minecraft plugin messaging channels, acting as the relay
 * hub for backend servers running [dev.gradienttim.gradeway.bukkit.messaging.
 * PluginMessageMessagingBroker] (which can never talk to each other directly). Every plugin
 * message received from a backend server is both dispatched locally (so this proxy's own caches
 * stay in sync) and rebroadcast to every backend server, including the origin - safe because
 * [CommonMessagingManager][dev.gradienttim.gradeway.managers.CommonMessagingManager] already
 * filters out self-originated payloads by `serverId`.
 *
 * [MessagingBroker] authenticates every message on top of the [Server] origin check already
 * performed in [onPluginMessage] - the backend side of this channel cannot tell a relayed message
 * from one sent directly by a connecting player's client, so it relies on that same shared secret.
 *
 * @property plugin The plugin used to register the channel, reach backend servers, and register
 * this broker's event listener under.
 */
class PluginMessageMessagingBroker(
    private val plugin: GradewayPlugin,
    messagingAuthenticator: MessagingAuthenticator,
) : MessagingBroker(messagingAuthenticator), Listener {
    override fun open() {
        plugin.proxy.registerChannel(MessagingConstants.SYNC_CHANNEL)
        plugin.proxy.pluginManager.registerListener(plugin, this)
    }

    override fun close() {
        plugin.proxy.pluginManager.unregisterListener(this)
        plugin.proxy.unregisterChannel(MessagingConstants.SYNC_CHANNEL)
    }

    override fun publishAuthenticated(channel: String, payload: ByteArray): Boolean {
        return plugin.proxy.servers.values.any { server ->
            server.sendData(channel, payload, true)
        }
    }

    override fun subscribeChannel(channel: String): Boolean = true

    @EventHandler
    fun onPluginMessage(event: PluginMessageEvent) {
        if (event.tag != MessagingConstants.SYNC_CHANNEL) return
        if (event.sender !is Server) return

        event.isCancelled = true

        val verifiedPayload = dispatch(event.data) ?: return
        publish(MessagingConstants.SYNC_CHANNEL, verifiedPayload)
    }
}
