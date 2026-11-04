/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.velocity.notification

import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import dev.gradienttim.gradeway.notification.drivers.ingame.IngameNotificationPlatform
import net.kyori.adventure.text.TranslatableComponent
import java.util.*

class VelocityIngameNotificationPlatform(
    private val server: ProxyServer,
) : IngameNotificationPlatform<Player> {
    override fun onlinePlayers(): Collection<Player> = server.allPlayers.toList()

    override fun uniqueId(player: Player): UUID = player.uniqueId

    override fun playerName(uniqueId: UUID): String? = server.getPlayer(uniqueId).map { it.username }.orElse(null)

    override fun hasPermission(player: Player, permission: String): Boolean = player.hasPermission(permission)

    override fun sendTranslatedMessage(player: Player, component: TranslatableComponent) {
        player.sendMessage(component)
    }
}
