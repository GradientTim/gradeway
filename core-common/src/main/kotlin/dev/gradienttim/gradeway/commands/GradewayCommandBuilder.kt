/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.commands

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import dev.gradienttim.gradeway.BuildInfo
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.command.command
import dev.gradienttim.gradeway.command.execute
import dev.gradienttim.gradeway.command.literal
import dev.gradienttim.gradeway.commands.gradeway.*
import dev.gradienttim.gradeway.extensions.formatUTC
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import java.time.Instant

fun <TSource> gradewayCommandBuilder(
    gradeway: CommonGradeway,
    literal: String,
    hasPermission: (source: TSource, permission: String) -> Boolean,
    sourceToAudience: (source: TSource) -> Audience,
): LiteralArgumentBuilder<TSource> {
    return command(literal) {
        roleBuilder(gradeway, hasPermission, sourceToAudience)
        groupBuilder(gradeway, hasPermission, sourceToAudience)
        playerBuilder(gradeway, hasPermission, sourceToAudience)
        backupBuilder(gradeway, hasPermission, sourceToAudience)
        migrationBuilder(literal, gradeway, hasPermission, sourceToAudience)
        permissionBuilder(gradeway, hasPermission, sourceToAudience)
        confirmationBuilder(gradeway, hasPermission, sourceToAudience)

        literal("reload") {
            requires { hasPermission(it, "gradeway.reload") }

            execute {
                val audience = sourceToAudience(source)

                gradeway.reload()
                    .onLeft { error ->
                        audience.sendMessage(
                            Component.translatable(
                                "gradeway.command.reload.failed",
                                Component.text(error.localizedMessage)
                            )
                        )
                    }
                    .onRight {
                        audience.sendMessage(Component.translatable("gradeway.command.reload.success"))
                    }
            }
        }

        execute {
            val audience = sourceToAudience(source)

            var commitHash = BuildInfo.GIT_COMMIT_HASH
            if (BuildInfo.GIT_IS_DIRTY) {
                commitHash += "-dirty"
            }

            val buildTimestamp = Instant
                .parse(BuildInfo.BUILD_TIMESTAMP)
                .formatUTC()

            audience.sendMessage(
                Component.translatable(
                    "gradeway.command.about.info",
                    Component.text(BuildInfo.VERSION),
                    Component.text(commitHash),
                    Component.text(buildTimestamp)
                )
            )
        }
    }
}
