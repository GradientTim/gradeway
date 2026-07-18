/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.bukkit.command

import dev.gradienttim.gradeway.bukkit.GradewayPlugin
import net.kyori.adventure.audience.Audience
import org.bukkit.command.CommandSender
import org.incendo.cloud.minecraft.extras.AudienceProvider

class BukkitAudienceProvider(val plugin: GradewayPlugin) : AudienceProvider<CommandSender> {
    override fun apply(sender: CommandSender): Audience {
        return plugin.adventure?.sender(sender)
            ?: error("Tried to access Adventure when the plugin was disabled!")
    }
}
