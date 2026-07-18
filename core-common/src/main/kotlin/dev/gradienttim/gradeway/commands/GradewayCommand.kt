/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.commands

import dev.gradienttim.gradeway.BuildInfo
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.commands.gradeway.*
import dev.gradienttim.gradeway.extensions.formatUTC
import net.kyori.adventure.text.Component
import org.incendo.cloud.CommandManager
import org.incendo.cloud.kotlin.extension.buildAndRegister
import org.incendo.cloud.minecraft.extras.AudienceProvider
import java.time.Instant

fun <C : Any> createGradewayCommand(
    literal: String,
    aliases: Array<String>,
    gradeway: CommonGradeway,
    commandManager: CommandManager<C>,
    audienceProvider: AudienceProvider<C>
) {
//     MinecraftHelp is currently binary-incompatible with Paper builds on Adventure 5.x
//     (cloud-minecraft-extras 2.0.0-beta.17 was compiled against adventure-api 4.15.0) and
//     throws a NoSuchMethodError from its pagination/header rendering. Re-enable once cloud
//     publishes a release built against Adventure 5.x.
//     val help = MinecraftHelp.create("/$literal help", commandManager, audienceProvider)

    commandManager.buildAndRegister(literal, aliases = aliases) {
        registerRoleCommand(gradeway, audienceProvider)
        registerGroupCommand(gradeway, audienceProvider)
        registerPlayerCommand(gradeway, audienceProvider)
        registerBackupCommand(gradeway, audienceProvider)
        registerMigrationCommand(literal, gradeway, audienceProvider)
        registerPermissionCommand(gradeway, audienceProvider)
        registerConfirmationCommand(gradeway, audienceProvider)

        registerCopy("reload") {
            permission("gradeway.reload")

            handler { context ->
                val audience = audienceProvider.apply(context.sender())

                gradeway.reload()
                    .onLeft { throwable ->
                        audience.sendMessage(
                            Component.translatable(
                                "gradeway.command.reload.failed",
                                Component.text(throwable.message ?: "Unknown")
                            )
                        )
                    }
                    .onRight {
                        audience.sendMessage(Component.translatable("gradeway.command.reload.success"))
                    }
            }
        }

//         See the MinecraftHelp comment above - re-enable once cloud publishes a release
//         built against Adventure 5.x.
//         registerCopy("help") {
//             optional("query", StringParser.greedyStringParser())
//
//             handler { context ->
//                 help.queryCommands(context.getOrDefault("query", ""), context.sender())
//             }
//         }

        handler { context ->
            val audience = audienceProvider.apply(context.sender())

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
