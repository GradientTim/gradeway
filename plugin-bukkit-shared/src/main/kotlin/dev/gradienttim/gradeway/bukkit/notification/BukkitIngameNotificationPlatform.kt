/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.bukkit.notification

import dev.gradienttim.gradeway.notification.drivers.ingame.IngameNotificationPlatform
import org.bukkit.Server
import org.bukkit.entity.Player
import java.util.*

/**
 * Shared Bukkit/Paper part of [IngameNotificationPlatform]. Only sending differs between the two
 * platforms, as Paper renders components natively while Bukkit needs legacy text.
 */
abstract class BukkitIngameNotificationPlatform(
    private val server: Server,
) : IngameNotificationPlatform<Player> {
    override fun onlinePlayers(): Collection<Player> = server.onlinePlayers.toList()

    override fun uniqueId(player: Player): UUID = player.uniqueId

    override fun playerName(uniqueId: UUID): String? = server.getPlayer(uniqueId)?.name

    override fun hasPermission(player: Player, permission: String): Boolean = player.hasPermission(permission)
}
