/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.commands.gradeway

import com.mojang.brigadier.builder.ArgumentBuilder
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.command.*
import dev.gradienttim.gradeway.command.context.CommandContext
import dev.gradienttim.gradeway.commands.extensions.requestConfirmation
import dev.gradienttim.gradeway.managers.BackupManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.translation.Argument
import kotlin.io.path.name

internal fun <TCommandSource> ArgumentBuilder<TCommandSource, *>.backupCommand(
    rootLiteral: String,
    gradeway: CommonGradeway<*>,
    commandContext: CommandContext<TCommandSource>,
) {
    fun handleImport(source: TCommandSource, fileName: String, wipe: Boolean = true) {
        requestConfirmation(source, commandContext, gradeway, rootLiteral) {
            gradeway.backups.import(fileName, wipe)
                .onLeft { error ->
                    if (error is BackupManager.ImportError.FileNotFound) {
                        commandContext.sendTranslatedMessage(
                            source,
                            Component.translatable(
                                "gradeway.command.backup.import.fileNotFound",
                                Argument.string("file", fileName)
                            )
                        )
                        return@requestConfirmation
                    }
                    if (error is BackupManager.ImportError.CorruptArchive) {
                        commandContext.sendTranslatedMessage(
                            source,
                            Component.translatable(
                                "gradeway.command.backup.import.corruptArchive",
                                Argument.string("file", fileName),
                                Argument.string("error", error.throwable.message ?: "Unknown")
                            )
                        )
                        return@requestConfirmation
                    }
                    if (error is BackupManager.ImportError.UnsupportedFormatVersion) {
                        commandContext.sendTranslatedMessage(
                            source,
                            Component.translatable(
                                "gradeway.command.backup.import.unsupportedFormatVersion",
                                Argument.string("file", fileName),
                                Argument.string("version", error.version.toString()),
                                Argument.string("supported", error.supportedVersion.toString())
                            )
                        )
                        return@requestConfirmation
                    }
                    if (error is BackupManager.ImportError.Unexpected) {
                        commandContext.sendTranslatedMessage(
                            source,
                            Component.translatable(
                                "gradeway.command.backup.import.unexpectedError",
                                Argument.string("file", fileName),
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
                            "gradeway.command.backup.import.success",
                            Argument.string("file", fileName)
                        )
                    )
                }
        }
    }

    literal("backup") {
        requires { commandContext.hasPermission(it, "gradeway.backup") }

        literal("export") {
            requires { commandContext.hasPermission(it, "gradeway.backup.export") }

            execute {
                gradeway.backups.export()
                    .onLeft { error ->
                        if (error is BackupManager.ExportError.FileAlreadyExists) {
                            commandContext.sendTranslatedMessage(
                                source,
                                Component.translatable(
                                    "gradeway.command.backup.export.fileAlreadyExists"
                                )
                            )
                            return@execute
                        }
                        if (error is BackupManager.ExportError.Unexpected) {
                            commandContext.sendTranslatedMessage(
                                source,
                                Component.translatable(
                                    "gradeway.command.backup.export.unexpectedError",
                                    Argument.string("error", error.throwable.message ?: "Unknown")
                                )
                            )
                            return@execute
                        }
                    }
                    .onRight { file ->
                        commandContext.sendTranslatedMessage(
                            source,
                            Component.translatable(
                                "gradeway.command.backup.export.success",
                                Argument.string("file", file.name)
                            )
                        )
                    }
            }
        }

        literal("import") {
            requires { commandContext.hasPermission(it, "gradeway.backup.import") }

            string("file") {
                execute {
                    val file = stringParam("file")

                    handleImport(source, file)
                }

                boolean("wipe") {
                    execute {
                        val file = stringParam("file")
                        val wipe = param("wipe", Boolean::class)

                        handleImport(source, file, wipe)
                    }
                }
            }
        }
    }
}
