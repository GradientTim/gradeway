/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.bukkit.command

import dev.gradienttim.gradeway.command.context.CommandContext
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TranslatableComponent
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.translation.GlobalTranslator
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.*

class BukkitCommandContext : CommandContext<CommandSender> {
    override fun sourceToUUID(source: CommandSender): UUID {
        if (source is Player) return source.uniqueId
        return super.sourceToUUID(source)
    }

    override fun hasPermission(source: CommandSender, permission: String): Boolean {
        return source.hasPermission(permission)
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
