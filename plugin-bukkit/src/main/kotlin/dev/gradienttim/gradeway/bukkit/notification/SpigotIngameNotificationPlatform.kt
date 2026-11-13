/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.bukkit.notification

import dev.gradienttim.gradeway.bukkit.command.context.BukkitCommandContext
import net.kyori.adventure.text.TranslatableComponent
import org.bukkit.Server
import org.bukkit.entity.Player

class SpigotIngameNotificationPlatform(
    server: Server,
    private val commandContext: BukkitCommandContext,
) : BukkitIngameNotificationPlatform(server) {
    override val supportsClickEvents: Boolean = false

    override fun sendTranslatedMessage(player: Player, component: TranslatableComponent) {
        commandContext.sendTranslatedMessage(player, component)
    }
}
