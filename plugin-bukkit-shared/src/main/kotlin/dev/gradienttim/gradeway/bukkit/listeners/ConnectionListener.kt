/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.bukkit.listeners

import dev.gradienttim.gradeway.GradewayLifecycle
import dev.gradienttim.gradeway.bukkit.permission.GradewayPermissibleBase
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles

class ConnectionListener(val gradeway: GradewayLifecycle) : Listener {
    private var entityPermissionHandle: MethodHandle? = null

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player

        gradeway.players.create(player.uniqueId, player.name)
        gradeway.players.removeExpiredRoles(player.uniqueId)
            .onLeft { error ->
                gradeway.logger.error("Failed to remove expired roles for ${player.name}: $error")
            }

        try {
            entityPermissionHandle?.invoke(player, GradewayPermissibleBase(gradeway, player))
            player.updateCommands()
        } catch (throwable: Throwable) {
            gradeway.logger.error(throwable.message ?: throwable::class.java.simpleName)
        }
    }

    private fun initializeEntityPermissionHandle() {
        try {
            val craftHumanEntityClass = Class.forName("org.bukkit.craftbukkit.entity.CraftHumanEntity")
            val permissionField = craftHumanEntityClass.getDeclaredField("perm")
            permissionField.isAccessible = true
            entityPermissionHandle = MethodHandles.lookup().unreflectSetter(permissionField)
        } catch (throwable: Throwable) {
            gradeway.logger.error(throwable.message ?: throwable::class.java.simpleName)
        }
    }

    init {
        initializeEntityPermissionHandle()
    }
}
