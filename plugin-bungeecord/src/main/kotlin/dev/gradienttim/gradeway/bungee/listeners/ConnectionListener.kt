/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.bungee.listeners

import dev.gradienttim.gradeway.GradewayLifecycle
import net.md_5.bungee.api.event.PostLoginEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.event.EventHandler

class ConnectionListener(val gradeway: GradewayLifecycle) : Listener {
    @EventHandler(priority = Byte.MAX_VALUE)
    fun onPostLogin(event: PostLoginEvent) {
        val player = event.player

        gradeway.players.create(player.uniqueId, player.name)
        gradeway.players.removeExpiredRoles(player.uniqueId)
            .onLeft { error ->
                gradeway.logger.error("Failed to remove expired roles for ${player.name}: $error")
            }
    }
}
