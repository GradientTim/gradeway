/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.velocity.command

import com.velocitypowered.api.command.CommandSource
import net.kyori.adventure.audience.Audience
import org.incendo.cloud.minecraft.extras.AudienceProvider

class VelocityAudienceProvider : AudienceProvider<CommandSource> {
    override fun apply(sender: CommandSource): Audience = sender
}
