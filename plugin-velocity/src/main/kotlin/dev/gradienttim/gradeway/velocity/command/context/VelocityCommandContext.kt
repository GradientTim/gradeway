/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.velocity.command.context

import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.proxy.Player
import dev.gradienttim.gradeway.command.context.CommandContext
import dev.gradienttim.gradeway.audit.Actor
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TranslatableComponent
import java.util.*

class VelocityCommandContext : CommandContext<CommandSource> {
    override fun sourceToUUID(source: CommandSource): UUID {
        if (source is Player) return source.uniqueId
        return super.sourceToUUID(source)
    }

    override fun sourceToActor(source: CommandSource): Actor {
        return when (source) {
            is Player -> Actor.Player(source.uniqueId)
            else -> Actor.Console
        }
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
