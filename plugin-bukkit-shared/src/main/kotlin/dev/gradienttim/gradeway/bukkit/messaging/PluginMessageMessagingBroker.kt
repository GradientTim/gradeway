/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.bukkit.messaging

import dev.gradienttim.gradeway.constants.MessagingConstants
import dev.gradienttim.gradeway.messaging.MessagingAuthenticator
import dev.gradienttim.gradeway.messaging.MessagingBroker
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.plugin.messaging.PluginMessageListener

/**
 * A [MessagingBroker] backed by vanilla Minecraft plugin messaging channels instead of an
 * external broker such as Redis. Requires a proxy (Velocity or, later, BungeeCord) relaying the
 * same channel to actually synchronize anything between backend servers - see
 * [PluginMessageDriver].
 *
 * Bukkit's incoming plugin-message API cannot tell a message relayed by the trusted proxy apart
 * from one sent directly by a connecting player's client, which is exactly the kind of transport
 * [MessagingBroker] always authenticates messages against, regardless of implementation.
 *
 * @property plugin The plugin used to register plugin channels and send/receive messages through.
 */
class PluginMessageMessagingBroker(
    private val plugin: JavaPlugin,
    messagingAuthenticator: MessagingAuthenticator,
) : MessagingBroker(messagingAuthenticator), PluginMessageListener {
    override fun open() {
        plugin.server.messenger.registerOutgoingPluginChannel(plugin, MessagingConstants.SYNC_CHANNEL)
        plugin.server.messenger.registerIncomingPluginChannel(plugin, MessagingConstants.SYNC_CHANNEL, this)
    }

    override fun close() {
        plugin.server.messenger.unregisterOutgoingPluginChannel(plugin, MessagingConstants.SYNC_CHANNEL)
        plugin.server.messenger.unregisterIncomingPluginChannel(plugin, MessagingConstants.SYNC_CHANNEL, this)
    }

    override fun publishAuthenticated(channel: String, payload: ByteArray): Boolean {
        // Server itself implements PluginMessageRecipient, so this goes through the server-level
        // API instead of manually picking a single online player to carry the message - Bukkit
        // delivers it via every currently connected, channel-listening player itself. A plugin
        // message packet always needs an active client connection to ride on, so this still fails
        // silently with zero online players.
        if (plugin.server.onlinePlayers.isEmpty()) return false
        plugin.server.sendPluginMessage(plugin, channel, payload)
        return true
    }

    override fun subscribeChannel(channel: String): Boolean = true

    override fun onPluginMessageReceived(channel: String, player: Player, message: ByteArray) {
        if (channel != MessagingConstants.SYNC_CHANNEL) return
        dispatch(message)
    }
}
