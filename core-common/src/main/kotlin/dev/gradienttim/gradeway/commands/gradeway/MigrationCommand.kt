/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.commands.gradeway

import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.managers.ConfirmationManager
import dev.gradienttim.gradeway.managers.MigrationManager
import dev.gradienttim.gradeway.registries.MigrationStrategyRegistry
import net.kyori.adventure.text.Component
import org.incendo.cloud.kotlin.MutableCommandBuilder
import org.incendo.cloud.kotlin.extension.suggestionProvider
import org.incendo.cloud.minecraft.extras.AudienceProvider
import org.incendo.cloud.parser.standard.StringParser.stringParser
import org.incendo.cloud.suggestion.SuggestionProvider

internal fun <C : Any> MutableCommandBuilder<C>.registerMigrationCommand(
    rootLiteral: String,
    gradeway: CommonGradeway,
    audienceProvider: AudienceProvider<C>,
) {
    registerCopy("migrate") {
        permission("gradeway.migrate")

        required("type", stringParser()) {
            suggestionProvider = SuggestionProvider.blockingStrings { _, _ ->
                MigrationStrategyRegistry.items.map { migrationStrategy -> migrationStrategy.type }
            }
        }
        required("file", stringParser())

        handler { context ->
            val audience = audienceProvider.apply(context.sender())

            val type = context.get<String>("type")
            val file = context.get<String>("file")

            val strategy = MigrationStrategyRegistry.find(type)
            if (strategy == null) {
                audience.sendMessage(
                    Component.translatable(
                        "gradeway.command.migrate.strategyNotRegistered",
                        Component.text(type)
                    )
                )
                return@handler
            }

            gradeway.confirmations.request(
                sender = audience,
                task = {
                    gradeway.migrations.migrate(strategy, file)
                        .onLeft { error ->
                            if (error is MigrationManager.MigrateError.FileNotFound) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.migrate.fileNotFound",
                                        Component.text(file)
                                    )
                                )
                                return@request
                            }
                            if (error is MigrationManager.MigrateError.Unexpected) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.migrate.unexpectedError",
                                        Component.text(error.throwable.message ?: "Unknown")
                                    )
                                )
                                return@request
                            }
                        }
                        .onRight {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.migrate.success",
                                    Component.text(type),
                                    Component.text(file)
                                )
                            )
                        }
                },
                onTimeout = { id ->
                    audience.sendMessage(
                        Component.translatable(
                            "gradeway.confirmation.timeout",
                            Component.text(id)
                        )
                    )
                }
            ).onLeft { error ->
                if (error is ConfirmationManager.RequestJobError.FailedToRegister) {
                    audience.sendMessage(
                        Component.translatable(
                            "gradeway.confirmation.request.failedToRegister"
                        )
                    )
                    return@handler
                }
                if (error is ConfirmationManager.RequestJobError.Unexpected) {
                    audience.sendMessage(
                        Component.translatable(
                            "gradeway.confirmation.request.unexpectedError",
                            Component.text(error.throwable.message ?: "Unknown")
                        )
                    )
                    return@handler
                }
            }.onRight { id ->
                audience.sendMessage(
                    Component.translatable(
                        "gradeway.confirmation.request.success",
                        Component.text(rootLiteral),
                        Component.text(id)
                    )
                )
            }
        }
    }
}
