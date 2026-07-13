/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.bukkit.messaging

import dev.gradienttim.gradeway.constants.MessagingConstants
import dev.gradienttim.gradeway.messaging.MessagingBroker
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.plugin.messaging.PluginMessageListener

/**
 * A [MessagingBroker] backed by vanilla Minecraft plugin messaging channels instead of an
 * external broker such as Redis. Requires a proxy (Velocity or, later, BungeeCord) relaying the
 * same channel to actually synchronize anything between backend servers - see
 * [dev.gradienttim.gradeway.bukkit.messaging.BukkitPluginMessageDriver].
 *
 * @property plugin The plugin used to register plugin channels and send/receive messages through.
 */
class BukkitPluginMessageMessagingBroker(
    private val plugin: JavaPlugin,
) : MessagingBroker, PluginMessageListener {
    private var listener: ((payload: ByteArray) -> Unit)? = null

    override fun open() {
        plugin.server.messenger.registerOutgoingPluginChannel(plugin, MessagingConstants.SYNC_CHANNEL)
        plugin.server.messenger.registerIncomingPluginChannel(plugin, MessagingConstants.SYNC_CHANNEL, this)
    }

    override fun close() {
        plugin.server.messenger.unregisterOutgoingPluginChannel(plugin, MessagingConstants.SYNC_CHANNEL)
        plugin.server.messenger.unregisterIncomingPluginChannel(plugin, MessagingConstants.SYNC_CHANNEL, this)
    }

    override fun publish(channel: String, payload: ByteArray): Boolean {
        // Server itself implements PluginMessageRecipient, so this goes through the server-level
        // API instead of manually picking a single online player to carry the message - Bukkit
        // delivers it via every currently connected, channel-listening player itself. A plugin
        // message packet always needs an active client connection to ride on, so this still fails
        // silently with zero online players.
        if (plugin.server.onlinePlayers.isEmpty()) return false
        plugin.server.sendPluginMessage(plugin, channel, payload)
        return true
    }

    override fun subscribe(channel: String, listener: (payload: ByteArray) -> Unit): Boolean {
        this.listener = listener
        return true
    }

    override fun onPluginMessageReceived(channel: String, player: Player, message: ByteArray) {
        if (channel == MessagingConstants.SYNC_CHANNEL) {
            listener?.invoke(message)
        }
    }
}
