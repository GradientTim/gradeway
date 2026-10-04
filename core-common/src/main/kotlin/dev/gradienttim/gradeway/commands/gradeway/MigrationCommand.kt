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
import dev.gradienttim.gradeway.command.string
import dev.gradienttim.gradeway.command.stringParam
import dev.gradienttim.gradeway.commands.extensions.requestConfirmation
import dev.gradienttim.gradeway.commands.extensions.suggestStrings
import dev.gradienttim.gradeway.managers.MigrationManager
import dev.gradienttim.gradeway.registries.MigrationStrategyRegistry
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.translation.Argument

internal fun <TCommandSource> ArgumentBuilder<TCommandSource, *>.migrationCommand(
    rootLiteral: String,
    gradeway: CommonGradeway<*>,
    commandContext: CommandContext<TCommandSource>,
) {
    literal("migrate") {
        requires { commandContext.hasPermission(it, "gradeway.migrate") }

        string("type") {
            suggestStrings { MigrationStrategyRegistry.items.map { migrationStrategy -> migrationStrategy.type } }

            string("file") {
                execute {
                    val type = stringParam("type")
                    val file = stringParam("file")

                    val strategy = MigrationStrategyRegistry.find(type)
                    if (strategy == null) {
                        commandContext.sendTranslatedMessage(
                            source,
                            Component.translatable(
                                "gradeway.command.migrate.strategyNotRegistered",
                                Argument.string("strategy", type)
                            )
                        )
                        return@execute
                    }

                    requestConfirmation(source, commandContext, gradeway, rootLiteral) {
                        gradeway.migrations.migrate(strategy, file)
                            .onLeft { error ->
                                if (error is MigrationManager.MigrateError.FileNotFound) {
                                    commandContext.sendTranslatedMessage(
                                        source,
                                        Component.translatable(
                                            "gradeway.command.migrate.fileNotFound",
                                            Argument.string("file", file)
                                        )
                                    )
                                    return@requestConfirmation
                                }
                                if (error is MigrationManager.MigrateError.Unexpected) {
                                    commandContext.sendTranslatedMessage(
                                        source,
                                        Component.translatable(
                                            "gradeway.command.migrate.unexpectedError",
                                            Argument.string("error", error.throwable.message ?: "Unknown")
                                        )
                                    )
                                    return@requestConfirmation
                                }
                            }
                            .onRight {
                                commandContext.sendTranslatedMessage(
                                    source,
                                    Component.translatable(
                                        "gradeway.command.migrate.success",
                                        Argument.string("strategy", type),
                                        Argument.string("file", file)
                                    )
                                )
                            }
                    }
                }
            }
        }
    }
}
