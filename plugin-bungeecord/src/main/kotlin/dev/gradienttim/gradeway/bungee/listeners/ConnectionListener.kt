/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.bungee.listeners

import dev.gradienttim.gradeway.GradewayLifecycle
import dev.gradienttim.gradeway.audit.Actor
import net.md_5.bungee.api.event.PlayerDisconnectEvent
import net.md_5.bungee.api.event.PostLoginEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.event.EventHandler

class ConnectionListener(val gradeway: GradewayLifecycle<*>) : Listener {
    @EventHandler(priority = Byte.MAX_VALUE)
    fun onPostLogin(event: PostLoginEvent) {
        val player = event.player

        val firstJoin = gradeway.players.create(Actor.System, player.uniqueId, player.name).isRight()
        gradeway.players.removeExpiredRoles(Actor.System, player.uniqueId)
            .onLeft { error ->
                gradeway.logger.error("Failed to remove expired roles for ${player.name}: $error")
            }
        gradeway.players.applyDefaultRole(Actor.System, player.uniqueId, firstJoin)
            .onLeft { error ->
                gradeway.logger.error("Failed to apply the default role for ${player.name}: $error")
            }
    }

    @EventHandler(priority = Byte.MAX_VALUE)
    fun onPlayerDisconnect(event: PlayerDisconnectEvent) {
        gradeway.caches.invalidatePlayer(event.player.uniqueId)
    }
}
