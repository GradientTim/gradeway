/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.commands

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.EnvironmentMeta
import dev.gradienttim.gradeway.GitMeta
import dev.gradienttim.gradeway.ProjectMeta
import dev.gradienttim.gradeway.command.command
import dev.gradienttim.gradeway.command.context.CommandContext
import dev.gradienttim.gradeway.command.execute
import dev.gradienttim.gradeway.command.literal
import dev.gradienttim.gradeway.commands.gradeway.*
import dev.gradienttim.gradeway.extensions.formatUTC
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.translation.Argument
import java.time.Instant

fun <TCommandSource> createGradewayCommand(
    literal: String,
    gradeway: CommonGradeway<*>,
    commandContext: CommandContext<TCommandSource>,
): LiteralArgumentBuilder<TCommandSource> {
    return command(literal) {
        roleCommand(literal, gradeway, commandContext)
        groupCommand(literal, gradeway, commandContext)
        trackCommand(literal, gradeway, commandContext)
        cacheCommand(gradeway, commandContext)
        playerCommand(literal, gradeway, commandContext)
        backupCommand(literal, gradeway, commandContext)
        migrationCommand(literal, gradeway, commandContext)
        permissionCommand(literal, gradeway, commandContext)
        confirmationCommand(gradeway, commandContext)

        literal("reload") {
            requires { commandContext.hasPermission(it, "gradeway.reload") }

            execute {
                gradeway.reload()
                    .onLeft { throwable ->
                        commandContext.sendTranslatedMessage(
                            source,
                            Component.translatable(
                                "gradeway.command.reload.failed",
                                Argument.string("error", throwable.message ?: "Unknown")
                            )
                        )
                    }
                    .onRight {
                        commandContext.sendTranslatedMessage(
                            source,
                            Component.translatable("gradeway.command.reload.success")
                        )
                    }
            }
        }

        execute {
            var commitHash = GitMeta.COMMIT_HASH_SHORT
            if (GitMeta.IS_DIRTY) {
                commitHash += "-dirty"
            }

            commandContext.sendTranslatedMessage(
                source, Component.translatable(
                    "gradeway.command.about.info",
                    Argument.string("version", ProjectMeta.VERSION),
                    Argument.string("commit", commitHash),
                    Argument.string("built", Instant.ofEpochSecond(EnvironmentMeta.BUILD_TIMESTAMP).formatUTC())
                )
            )
        }
    }
}
