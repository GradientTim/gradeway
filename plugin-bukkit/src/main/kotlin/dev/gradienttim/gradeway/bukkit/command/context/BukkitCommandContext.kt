/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.bukkit.command.context

import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.bukkit.config.BukkitPlatformConfig
import dev.gradienttim.gradeway.command.context.CommandContext
import dev.gradienttim.gradeway.audit.Actor
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TranslatableComponent
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.translation.GlobalTranslator
import org.bukkit.command.BlockCommandSender
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.entity.Player
import java.util.*

class BukkitCommandContext(val gradeway: CommonGradeway<BukkitPlatformConfig>) : CommandContext<CommandSender> {
    override fun sourceToUUID(source: CommandSender): UUID {
        if (source is Player) return source.uniqueId
        return super.sourceToUUID(source)
    }

    override fun sourceToActor(source: CommandSender): Actor {
        return when (source) {
            is Player -> Actor.Player(source.uniqueId)
            is ConsoleCommandSender -> Actor.Console
            is BlockCommandSender -> {
                val block = source.block
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

    override fun hasPermission(source: CommandSender, permission: String): Boolean {
        return when (source) {
            is BlockCommandSender -> gradeway.configs.platformEntry.config.disableCommandBlocks
            else -> source.hasPermission(permission)
        }
    }

    override fun sendMessage(source: CommandSender, component: Component) {
        val serializer = if (source is Player) {
            PLAYER_LEGACY_COMPONENT_SERIALIZER
        } else {
            CONSOLE_LEGACY_COMPONENT_SERIALIZER
        }

        source.sendMessage(serializer.serialize(component))
    }

    override fun sendTranslatedMessage(source: CommandSender, component: TranslatableComponent) {
        val locale = if (source is Player) Locale.forLanguageTag(source.locale.replace('_', '-')) else Locale.US
        val translatedComponent = GlobalTranslator.renderer().render(component, locale)

        sendMessage(source, translatedComponent)
    }

    companion object {
        val PLAYER_LEGACY_COMPONENT_SERIALIZER: LegacyComponentSerializer = LegacyComponentSerializer.builder()
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build()
        val CONSOLE_LEGACY_COMPONENT_SERIALIZER: LegacyComponentSerializer = LegacyComponentSerializer.legacySection()
    }
}
