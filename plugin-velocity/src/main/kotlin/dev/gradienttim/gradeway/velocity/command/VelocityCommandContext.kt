/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.velocity.command

import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.proxy.Player
import dev.gradienttim.gradeway.command.context.CommandContext
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TranslatableComponent
import java.util.*

class VelocityCommandContext : CommandContext<CommandSource> {
    override fun sourceToUUID(source: CommandSource): UUID {
        if (source is Player) return source.uniqueId
        return super.sourceToUUID(source)
    }

    override fun hasPermission(source: CommandSource, permission: String): Boolean =
        source.hasPermission(permission)

    override fun sendMessage(source: CommandSource, component: Component) {
        source.sendMessage(component)
    }

    override fun sendTranslatedMessage(source: CommandSource, component: TranslatableComponent) {
        sendMessage(source, component)
    }
}
