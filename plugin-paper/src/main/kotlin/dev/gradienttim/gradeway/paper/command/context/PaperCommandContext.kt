/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.paper.command.context

import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.bukkit.config.BukkitPlatformConfig
import dev.gradienttim.gradeway.command.context.CommandContext
import dev.gradienttim.gradeway.audit.Actor
import io.papermc.paper.command.brigadier.CommandSourceStack
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TranslatableComponent
import org.bukkit.command.BlockCommandSender
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.entity.Player
import java.util.*

class PaperCommandContext(val gradeway: CommonGradeway<BukkitPlatformConfig>) : CommandContext<CommandSourceStack> {
    override fun sourceToUUID(source: CommandSourceStack): UUID {
        val sender = source.sender
        if (sender is Player) return sender.uniqueId
        return super.sourceToUUID(source)
    }

    override fun sourceToActor(source: CommandSourceStack): Actor {
        return when (val sender = source.executor ?: source.sender) {
            is Player -> Actor.Player(sender.uniqueId)
            is ConsoleCommandSender -> Actor.Console
            is BlockCommandSender -> {
                val block = sender.block
                Actor.CommandBlock(
                    block.world.name,
                    block.location.blockX,
                    block.location.blockY,
                    block.location.blockZ
                )
            }

            else -> Actor.Console
        }
    }

    override fun hasPermission(source: CommandSourceStack, permission: String): Boolean {
        val sender = source.executor ?: source.sender
        return when (sender) {
            is BlockCommandSender -> !gradeway.configs.platformEntry.config.disableCommandBlocks
            else -> sender.hasPermission(permission)
        }
    }

    override fun sendMessage(source: CommandSourceStack, component: Component) {
        source.sender.sendMessage(component)
    }

    override fun sendTranslatedMessage(source: CommandSourceStack, component: TranslatableComponent) {
        sendMessage(source, component)
    }
}
