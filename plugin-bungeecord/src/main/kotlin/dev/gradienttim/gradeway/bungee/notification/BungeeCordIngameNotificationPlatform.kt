/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.bungee.notification

import dev.gradienttim.gradeway.bungee.command.context.BungeeCommandContext
import dev.gradienttim.gradeway.notification.drivers.ingame.IngameNotificationPlatform
import net.kyori.adventure.text.TranslatableComponent
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.connection.ProxiedPlayer
import java.util.*

class BungeeCordIngameNotificationPlatform(
    private val proxy: ProxyServer,
    private val commandContext: BungeeCommandContext,
) : IngameNotificationPlatform<ProxiedPlayer> {
    override fun onlinePlayers(): Collection<ProxiedPlayer> = proxy.players.toList()

    override fun uniqueId(player: ProxiedPlayer): UUID = player.uniqueId

    override fun playerName(uniqueId: UUID): String? = proxy.getPlayer(uniqueId)?.name

    override fun hasPermission(player: ProxiedPlayer, permission: String): Boolean = player.hasPermission(permission)

    override fun sendTranslatedMessage(player: ProxiedPlayer, component: TranslatableComponent) {
        commandContext.sendTranslatedMessage(player, component)
    }
}
