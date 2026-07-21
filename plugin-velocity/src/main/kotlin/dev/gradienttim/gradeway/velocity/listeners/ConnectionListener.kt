/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.velocity.listeners

import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.DisconnectEvent
import com.velocitypowered.api.event.connection.LoginEvent
import dev.gradienttim.gradeway.GradewayLifecycle

class ConnectionListener(val gradeway: GradewayLifecycle) {
    @Subscribe(order = PostOrder.EARLY, priority = Short.MAX_VALUE)
    fun onLogin(event: LoginEvent) {
        val player = event.player
        gradeway.players.create(player.uniqueId, player.username)

        gradeway.players.removeExpiredRoles(player.uniqueId)
            .onLeft { error -> gradeway.logger.error("Failed to remove expired roles for ${player.username}: $error") }
    }

    @Subscribe(order = PostOrder.LATE, priority = Short.MIN_VALUE)
    fun onDisconnect(event: DisconnectEvent) {
        gradeway.caches.invalidatePlayer(event.player.uniqueId)
    }
}
