/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.bungee.command

import dev.gradienttim.gradeway.bungee.GradewayPlugin
import net.kyori.adventure.audience.Audience
import net.md_5.bungee.api.CommandSender
import org.incendo.cloud.minecraft.extras.AudienceProvider

class BungeeAudienceProvider(val plugin: GradewayPlugin) : AudienceProvider<CommandSender> {
    override fun apply(sender: CommandSender): Audience {
        return plugin.adventure?.sender(sender)
            ?: error("Cannot retrieve audience provider while plugin is not enabled")
    }
}
