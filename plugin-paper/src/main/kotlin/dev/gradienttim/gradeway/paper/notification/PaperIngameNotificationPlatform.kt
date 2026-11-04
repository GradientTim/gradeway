/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.paper.notification

import dev.gradienttim.gradeway.bukkit.notification.BukkitIngameNotificationPlatform
import net.kyori.adventure.text.TranslatableComponent
import org.bukkit.Server
import org.bukkit.entity.Player

class PaperIngameNotificationPlatform(
    server: Server,
) : BukkitIngameNotificationPlatform(server) {
    override fun sendTranslatedMessage(player: Player, component: TranslatableComponent) {
        player.sendMessage(component)
    }
}
