/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.paper.command.context

import dev.gradienttim.gradeway.command.context.CommandContext
import io.papermc.paper.command.brigadier.CommandSourceStack
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TranslatableComponent
import org.bukkit.entity.Player
import java.util.*

class PaperCommandContext : CommandContext<CommandSourceStack> {
    override fun sourceToUUID(source: CommandSourceStack): UUID {
        val sender = source.sender
        if (sender is Player) return sender.uniqueId
        return super.sourceToUUID(source)
    }

    override fun hasPermission(source: CommandSourceStack, permission: String): Boolean {
        return source.sender.hasPermission(permission)
    }

    override fun sendMessage(source: CommandSourceStack, component: Component) {
        source.sender.sendMessage(component)
    }

    override fun sendTranslatedMessage(source: CommandSourceStack, component: TranslatableComponent) {
        sendMessage(source, component)
    }
}
