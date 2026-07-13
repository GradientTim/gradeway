/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.commands.gradeway

import com.mojang.brigadier.builder.ArgumentBuilder
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.command.execute
import dev.gradienttim.gradeway.command.literal
import dev.gradienttim.gradeway.command.string
import dev.gradienttim.gradeway.command.stringParam
import dev.gradienttim.gradeway.managers.ConfirmationManager
import dev.gradienttim.gradeway.managers.MigrationManager
import dev.gradienttim.gradeway.registries.MigrationStrategyRegistry
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component

internal fun <TSource> ArgumentBuilder<TSource, *>.migrationBuilder(
    rootLiteral: String,
    gradeway: CommonGradeway,
    hasPermission: (source: TSource, permission: String) -> Boolean,
    sourceToAudience: (source: TSource) -> Audience,
) {
    literal("migrate") {
        requires { hasPermission(it, "gradeway.migrate") }

        string("type") {
            suggests { _, builder ->
                MigrationStrategyRegistry.items.forEach { migrationStrategy ->
                    builder.suggest(migrationStrategy.type)
                }
                builder.buildFuture()
            }

            string("file") {
                execute {
                    val audience = sourceToAudience(source)

                    val type = stringParam("type")
                    val file = stringParam("file")

                    val strategy = MigrationStrategyRegistry.find(type)
                    if (strategy == null) {
                        audience.sendMessage(
                            Component.translatable(
                                "gradeway.command.migrate.strategyNotRegistered",
                                Component.text(type)
                            )
                        )
                        return@execute
                    }

                    gradeway.confirmations.request(
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
                            return@execute
                        }
                        if (error is ConfirmationManager.RequestJobError.Unexpected) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.confirmation.request.unexpectedError",
                                    Component.text(error.throwable.message ?: "Unknown")
                                )
                            )
                            return@execute
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
    }
}
