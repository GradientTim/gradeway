/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.commands

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.command.command
import dev.gradienttim.gradeway.command.execute
import dev.gradienttim.gradeway.command.literal
import dev.gradienttim.gradeway.commands.gradeway.*
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component

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
            audience.sendMessage(Component.translatable("gradeway.command.usage.version"))
            if (hasPermission(source, "gradeway.usage")) {
                audience.sendMessage(Component.translatable("gradeway.command.usage.help"))
            }
        }
    }
}
