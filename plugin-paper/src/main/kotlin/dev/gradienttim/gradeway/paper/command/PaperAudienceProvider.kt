/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.paper.command

import io.papermc.paper.command.brigadier.CommandSourceStack
import net.kyori.adventure.audience.Audience
import org.incendo.cloud.minecraft.extras.AudienceProvider

class PaperAudienceProvider : AudienceProvider<CommandSourceStack> {
    override fun apply(sender: CommandSourceStack): Audience = sender.sender
}
