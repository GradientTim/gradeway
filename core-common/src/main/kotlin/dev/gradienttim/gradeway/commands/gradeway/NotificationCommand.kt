/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.commands.gradeway

import com.mojang.brigadier.builder.ArgumentBuilder
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.audit.Actor
import dev.gradienttim.gradeway.command.context.CommandContext
import dev.gradienttim.gradeway.command.execute
import dev.gradienttim.gradeway.command.literal
import dev.gradienttim.gradeway.notification.drivers.ingame.IngameNotificationNotifier
import dev.gradienttim.gradeway.utilities.Store
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.translation.Argument

internal fun <TCommandSource> ArgumentBuilder<TCommandSource, *>.notificationCommand(
    gradeway: CommonGradeway<*>,
    commandContext: CommandContext<TCommandSource>,
) {
    fun handleReceiving(source: TCommandSource, state: Boolean? = null) {
        // Notifications are delivered as chat messages to online players, so the console, RCON,
        // command blocks and other non-player sources have nothing to receive them with.
        val player = commandContext.sourceToActor(source) as? Actor.Player
        if (player == null) {
            commandContext.sendTranslatedMessage(
                source,
                Component.translatable("gradeway.command.notify.playersOnly"),
            )
            return
        }

        val result = if (state == null) {
            gradeway.notifications.toggleReceiving(player.uniqueId)
        } else {
            gradeway.notifications.setReceiving(player.uniqueId, state)
        }

        result.onLeft { error ->
            when (error) {
                is Store.SetError.Unexpected -> {
                    commandContext.sendTranslatedMessage(
                        source,
                        Component.translatable(
                            "gradeway.command.notify.toggle.unexpectedError",
                            Argument.string("error", error.throwable.message ?: "Unknown")
                        )
                    )
                }
            }
        }.onRight { receiving ->
            val key = if (receiving) {
                "gradeway.command.notify.toggle.enabled"
            } else {
                "gradeway.command.notify.toggle.disabled"
            }

            commandContext.sendTranslatedMessage(source, Component.translatable(key))
        }
    }

    literal("notify") {
        requires { commandContext.hasPermission(it, IngameNotificationNotifier.PERMISSION) }

        literal("on") {
            execute { handleReceiving(source, true) }
        }

        literal("off") {
            execute { handleReceiving(source, false) }
        }

        execute { handleReceiving(source) }
    }
}
