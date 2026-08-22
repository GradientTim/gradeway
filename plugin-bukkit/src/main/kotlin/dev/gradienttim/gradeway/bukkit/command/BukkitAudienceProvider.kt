/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.bukkit.command

import dev.gradienttim.gradeway.bukkit.GradewayBukkitInstance
import net.kyori.adventure.audience.Audience
import org.bukkit.command.CommandSender
import org.incendo.cloud.minecraft.extras.AudienceProvider

class BukkitAudienceProvider(val instance: GradewayBukkitInstance) : AudienceProvider<CommandSender> {
    override fun apply(sender: CommandSender): Audience {
        return instance.adventure?.sender(sender)
            ?: error("Tried to access Adventure when the plugin was disabled!")
    }
}
