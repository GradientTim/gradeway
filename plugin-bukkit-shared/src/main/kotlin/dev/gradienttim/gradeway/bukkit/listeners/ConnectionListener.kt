/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.bukkit.listeners

import dev.gradienttim.gradeway.GradewayLifecycle
import dev.gradienttim.gradeway.bukkit.config.BukkitPlatformConfig
import dev.gradienttim.gradeway.bukkit.permission.GradewayPermissibleBase
import dev.gradienttim.gradeway.messaging.payloads.*
import org.bukkit.Server
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.util.*

class ConnectionListener(
    val server: Server,
    val gradeway: GradewayLifecycle<BukkitPlatformConfig>
) : Listener {
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
        } catch (throwable: Throwable) {
            gradeway.logger.error(throwable.message ?: throwable::class.java.simpleName)
        }

        player.recalculatePermissions()
        player.updateCommands()
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        gradeway.caches.invalidatePlayer(event.player.uniqueId)
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

    private fun onMessagingPayload(payload: MessagingPayload) {
        when (payload) {
            is PlayerRoleChangedPayload -> refreshCommandsFor(payload.playerId)
            is PlayerPermissionChangedPayload -> refreshCommandsFor(payload.playerId)
            is PlayerPermissionsClearedPayload -> refreshCommandsFor(payload.playerId)

            is PlayerChangedPayload,
            is PlayerAttributeChangedPayload,
            is PlayerAttributesClearedPayload -> Unit

            is RoleChangedPayload,
            is RolePermissionChangedPayload,
            is RolePermissionsClearedPayload,
            is RoleParentChangedPayload,
            is GroupChangedPayload,
            is GroupRoleChangedPayload,
            is GroupPermissionChangedPayload,
            is GroupPermissionsClearedPayload,
            is PermissionChangedPayload,
            is PermissionValueChangedPayload,
            is PermissionTypeChangedPayload,
            is PermissionTemplateChangedPayload,
            is PermissionTemplatePermissionChangedPayload,
            is PermissionTemplatePermissionsClearedPayload,
            is PermissionTemplateRoleLinkChangedPayload,
            is PermissionTemplateGroupLinkChangedPayload,
            is PermissionTemplatePlayerLinkChangedPayload,
            is CacheFlushPayload -> refreshAllCommands()

            is RoleAttributeChangedPayload,
            is RoleAttributesClearedPayload -> Unit
        }
    }

    private fun refreshCommandsFor(playerId: String) {
        val uuid = runCatching { UUID.fromString(playerId) }.getOrNull() ?: return
        runOnMainThread {
            val player = server.getPlayer(uuid) ?: return@runOnMainThread
            refreshCommands(player)
        }
    }

    private fun refreshAllCommands() {
        runOnMainThread {
            server.onlinePlayers.forEach(::refreshCommands)
        }
    }

    private fun refreshCommands(player: Player) {
        player.recalculatePermissions()
        player.updateCommands()
    }

    private fun runOnMainThread(action: () -> Unit) {
        if (server.isPrimaryThread) {
            action()
        } else {
            gradeway.scheduler.runTask(action)
        }
    }

    init {
        initializeEntityPermissionHandle()
        gradeway.messaging.subscribe { payload -> onMessagingPayload(payload) }
    }
}
