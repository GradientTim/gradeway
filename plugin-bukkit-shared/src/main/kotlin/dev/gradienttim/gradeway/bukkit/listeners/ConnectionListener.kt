/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.bukkit.listeners

import dev.gradienttim.gradeway.GradewayLifecycle
import dev.gradienttim.gradeway.bukkit.config.BukkitPlatformConfig
import dev.gradienttim.gradeway.bukkit.permission.GradewayPermissibleBase
import dev.gradienttim.gradeway.messaging.payloads.*
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.java.JavaPlugin
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.util.*

class ConnectionListener(
    val gradeway: GradewayLifecycle<BukkitPlatformConfig>,
    private val plugin: JavaPlugin
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

        // Force a recalculation now that gradeway is attached, so a player who already has real
        // op status (e.g., from ops.json) gets de-opped immediately if disableOp is enabled, rather
        // than waiting for the next incidental permission recalculation.
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

    /**
     * Reacts to a [MessagingPayload] (whether it originated locally or on another server) by
     * refreshing the client-side command tree of every online player it could have changed the
     * effective permissions of. Bukkit only recomputes a player's visible command tree
     * (`/gradeway <tab>`, greyed-out subcommands, ...) when [Player.updateCommands] is called -
     * setting a permission through Gradeway's services never touches the vanilla permission
     * attachments Bukkit itself watches for that, so without this listener a player has to
     * reconnect to see the change reflected.
     *
     * Mirrors the exhaustive payload categorization in
     * `CommonPermissionService.invalidateFor`: player-specific payloads only refresh that one
     * online player, anything broader (a role, group, permission, or permission template
     * changing) refreshes every online player since tracing exactly who inherits it through
     * arbitrary role/group chains isn't worth it for an edit that is rare relative to a normal
     * command-tree refresh.
     */
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
            val player = plugin.server.getPlayer(uuid) ?: return@runOnMainThread
            refreshCommands(player)
        }
    }

    private fun refreshAllCommands() {
        runOnMainThread {
            plugin.server.onlinePlayers.forEach(::refreshCommands)
        }
    }

    private fun refreshCommands(player: Player) {
        player.recalculatePermissions()
        player.updateCommands()
    }

    /**
     * A [MessagingPayload] may arrive on a network broker's own thread (e.g. Redis's subscriber
     * thread) rather than the server's main thread, and both [Player.recalculatePermissions] and
     * [Player.updateCommands] must run on the main thread like any other Bukkit API call.
     */
    private fun runOnMainThread(action: () -> Unit) {
        if (plugin.server.isPrimaryThread) {
            action()
        } else {
            plugin.server.scheduler.runTask(plugin, Runnable(action))
        }
    }

    init {
        initializeEntityPermissionHandle()
        gradeway.messaging.subscribe { payload -> onMessagingPayload(payload) }
    }
}
