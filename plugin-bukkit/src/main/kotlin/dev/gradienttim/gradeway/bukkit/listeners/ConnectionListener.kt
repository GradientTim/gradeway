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
import org.bukkit.event.player.PlayerQuitEvent

class ConnectionListener(val plugin: GradewayPlugin) : Listener {
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        plugin.gradeway.players.create(player.uniqueId, player.name)
        plugin.registerPermissionAttachment(player)
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        plugin.unregisterPermissionAttachment(player)
    }
}
