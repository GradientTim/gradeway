/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.bungee.command

import dev.gradienttim.gradeway.command.context.CommandContext
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TranslatableComponent
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.translation.GlobalTranslator
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.connection.ProxiedPlayer
import net.md_5.bungee.chat.ComponentSerializer
import java.util.*

class BungeeCommandContext : CommandContext<CommandSender> {
    override fun sourceToUUID(source: CommandSender): UUID {
        if (source is ProxiedPlayer) return source.uniqueId
        return super.sourceToUUID(source)
    }

    override fun hasPermission(source: CommandSender, permission: String): Boolean {
        return source.hasPermission(permission)
    }

    override fun sendMessage(source: CommandSender, component: Component) {
        val renderedComponent = if (source is ProxiedPlayer) {
            component
        } else {
            LEGACY_COMPONENT_SERIALIZER.deserialize(LEGACY_COMPONENT_SERIALIZER.serialize(component))
        }

        val json = GSON_COMPONENT_SERIALIZER.serialize(renderedComponent)
        for (baseComponent in ComponentSerializer.parse(json)) {
            source.sendMessage(baseComponent)
        }
    }

    override fun sendTranslatedMessage(source: CommandSender, component: TranslatableComponent) {
        val locale = if (source is ProxiedPlayer) source.locale else Locale.US
        val translatedComponent = GlobalTranslator.renderer().render(component, locale)

        sendMessage(source, translatedComponent)
    }

    companion object {
        val GSON_COMPONENT_SERIALIZER: GsonComponentSerializer = GsonComponentSerializer.gson()
        val LEGACY_COMPONENT_SERIALIZER: LegacyComponentSerializer = LegacyComponentSerializer.legacySection()
    }
}
