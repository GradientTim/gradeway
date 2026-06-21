/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.listeners

import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.LoginEvent
import dev.gradienttim.gradeway.Gradeway

class ConnectionListener(val gradeway: Gradeway) {
    @Subscribe(order = PostOrder.EARLY, priority = Short.MAX_VALUE)
    fun onLogin(event: LoginEvent) {
        val player = event.player
        gradeway.players.create(player.uniqueId, player.username)
    }
}
