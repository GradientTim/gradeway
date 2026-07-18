/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.bungee.listeners

import dev.gradienttim.gradeway.Gradeway
import net.md_5.bungee.api.connection.ProxiedPlayer
import net.md_5.bungee.api.event.PermissionCheckEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.event.EventHandler

class PermissionListener(val gradeway: Gradeway) : Listener {
    @EventHandler(priority = Byte.MAX_VALUE)
    fun onPermissionCheck(event: PermissionCheckEvent) {
        val sender = event.sender
        val permission = event.permission

        if (sender is ProxiedPlayer) {
            event.setHasPermission(gradeway.permissions.hasEffectivePlayerPermission(sender.uniqueId, permission))
        }
    }
}
