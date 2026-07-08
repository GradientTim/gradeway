/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.bukkit.listeners

import dev.gradienttim.gradeway.bukkit.GradewayPlugin
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

class ConnectionListener(val plugin: GradewayPlugin) : Listener {
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player

        plugin.gradeway.players.create(player.uniqueId, player.name)
        plugin.gradeway.players.removeExpiredRoles(player.uniqueId)
            .onLeft { error -> plugin.slF4JLogger.error("Failed to remove expired roles for ${player.name}: $error") }

        try {
            plugin.applyEntityPermissions(player)
            player.updateCommands()
        } catch (throwable: Throwable) {
            plugin.slF4JLogger.error(throwable.localizedMessage, throwable)
        }
    }
}
