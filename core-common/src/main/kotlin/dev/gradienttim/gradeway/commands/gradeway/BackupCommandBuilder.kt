/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.commands.gradeway

import com.mojang.brigadier.builder.ArgumentBuilder
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.command.*
import dev.gradienttim.gradeway.managers.BackupManager
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component

internal fun <TSource> ArgumentBuilder<TSource, *>.backupBuilder(
    gradeway: CommonGradeway,
    hasPermission: (source: TSource, permission: String) -> Boolean,
    sourceToAudience: (source: TSource) -> Audience,
) {
    fun handleImport(audience: Audience, fileName: String, wipe: Boolean = true) {
        gradeway.backups.import(fileName, wipe)
            .onLeft { error ->
                if (error is BackupManager.ImportError.FileNotFound) {
                    audience.sendMessage(
                        Component.translatable(
                            "gradeway.command.backup.import.fileNotFound",
                            Component.text(fileName)
                        )
                    )
                    return
                }
                if (error is BackupManager.ImportError.CorruptArchive) {
                    audience.sendMessage(
                        Component.translatable(
                            "gradeway.command.backup.import.corruptArchive",
                            Component.text(fileName),
                            Component.text(error.throwable.message ?: "Unknown")
                        )
                    )
                    return
                }
                if (error is BackupManager.ImportError.Unexpected) {
                    audience.sendMessage(
                        Component.translatable(
                            "gradeway.command.backup.import.unexpectedError",
                            Component.text(fileName),
                            Component.text(error.throwable.message ?: "Unknown")
                        )
                    )
                    return
                }
            }
            .onRight {
                audience.sendMessage(
                    Component.translatable(
                        "gradeway.command.backup.import.success",
                        Component.text(fileName)
                    )
                )
            }
    }

    literal("backup") {
        literal("export") {
            requires { hasPermission(it, "gradeway.backup.export") }

            execute {
                val audience = sourceToAudience(source)

                gradeway.backups.export()
                    .onLeft { error ->
                        if (error is BackupManager.ExportError.FileAlreadyExists) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.backup.export.fileAlreadyExists"
                                )
                            )
                            return@execute
                        }
                        if (error is BackupManager.ExportError.Unexpected) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.backup.export.unexpectedError",
                                    Component.text(error.throwable.message ?: "Unknown")
                                )
                            )
                            return@execute
                        }
                    }
                    .onRight { file ->
                        audience.sendMessage(
                            Component.translatable(
                                "gradeway.command.backup.export.success",
                                Component.text(file.name)
                            )
                        )
                    }
            }
        }

        literal("import") {
            requires { hasPermission(it, "gradeway.backup.import") }

            string("file") {
                boolean("wipe") {
                    execute {
                        val audience = sourceToAudience(source)

                        val file = stringParam("file")
                        val wipe = booleanParam("wipe")

                        handleImport(audience, file, wipe)
                    }
                }

                execute {
                    val audience = sourceToAudience(source)

                    val file = stringParam("file")

                    handleImport(audience, file)
                }
            }
        }
    }
}
