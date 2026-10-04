/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.commands.gradeway

import com.mojang.brigadier.builder.ArgumentBuilder
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.command.context.CommandContext
import dev.gradienttim.gradeway.command.execute
import dev.gradienttim.gradeway.command.literal
import dev.gradienttim.gradeway.command.param
import dev.gradienttim.gradeway.command.string
import dev.gradienttim.gradeway.commands.extensions.suggestStrings
import dev.gradienttim.gradeway.platform.Caches
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.translation.Argument

internal fun <TCommandSource> ArgumentBuilder<TCommandSource, *>.cacheCommand(
    gradeway: CommonGradeway<*>,
    commandContext: CommandContext<TCommandSource>,
) {
    fun flush(source: TCommandSource, type: Caches.Type) {
        type.run(gradeway.caches)
            .onLeft { throwable ->
                commandContext.sendTranslatedMessage(
                    source,
                    Component.translatable(
                        "gradeway.cache.flush.failed",
                        Argument.string("cache", type.name),
                        Argument.string("error", throwable.message ?: throwable::class.java.simpleName)
                    )
                )
            }
            .onRight {
                commandContext.sendTranslatedMessage(
                    source,
                    Component.translatable(
                        "gradeway.cache.flush.success",
                        Argument.string("cache", type.name)
                    )
                )
            }
    }

    literal("cache") {
        requires { commandContext.hasPermission(it, "gradeway.cache") }

        literal("flush") {
            requires { commandContext.hasPermission(it, "gradeway.cache.flush") }

            execute {
                flush(source, Caches.Type.ALL)
            }

            string("type") {
                suggestStrings { Caches.Type.entries.map { it.name } }

                execute {
                    val rawType = param("type", String::class).uppercase()
                    val type = Caches.Type.entries.find { it.name == rawType } ?: Caches.Type.ALL

                    flush(source, type)
                }
            }
        }
    }
}
