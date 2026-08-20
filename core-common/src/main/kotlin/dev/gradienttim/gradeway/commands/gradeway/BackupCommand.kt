/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.commands.gradeway

import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.managers.BackupManager
import dev.gradienttim.gradeway.managers.ConfirmationManager
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import org.incendo.cloud.kotlin.MutableCommandBuilder
import org.incendo.cloud.minecraft.extras.AudienceProvider
import org.incendo.cloud.parser.standard.BooleanParser.booleanParser
import org.incendo.cloud.parser.standard.StringParser.stringParser

internal fun <C : Any> MutableCommandBuilder<C>.registerBackupCommand(
    rootLiteral: String,
    gradeway: CommonGradeway<*>,
    audienceProvider: AudienceProvider<C>,
) {
    fun handleImport(audience: Audience, fileName: String, wipe: Boolean = true) {
        gradeway.confirmations.request(
            sender = audience,
            handler = {
                gradeway.backups.import(fileName, wipe)
                    .onLeft { error ->
                        if (error is BackupManager.ImportError.FileNotFound) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.backup.import.fileNotFound",
                                    Component.text(fileName)
                                )
                            )
                            return@request
                        }
                        if (error is BackupManager.ImportError.CorruptArchive) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.backup.import.corruptArchive",
                                    Component.text(fileName),
                                    Component.text(error.throwable.message ?: "Unknown")
                                )
                            )
                            return@request
                        }
                        if (error is BackupManager.ImportError.Unexpected) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.backup.import.unexpectedError",
                                    Component.text(fileName),
                                    Component.text(error.throwable.message ?: "Unknown")
                                )
                            )
                            return@request
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
            },
            onTimeout = { jobId ->
                audience.sendMessage(
                    Component.translatable(
                        "gradeway.confirmation.timeout",
                        Component.text(jobId)
                    )
                )
            }
        ).onLeft { error ->
            if (error is ConfirmationManager.RequestJobError.FailedToRegister) {
                audience.sendMessage(
                    Component.translatable("gradeway.confirmation.request.failedToRegister")
                )
                return
            }
            if (error is ConfirmationManager.RequestJobError.Unexpected) {
                audience.sendMessage(
                    Component.translatable(
                        "gradeway.confirmation.request.unexpectedError",
                        Component.text(error.throwable.message ?: "Unknown")
                    )
                )
                return
            }
        }.onRight { jobId ->
            audience.sendMessage(
                Component.translatable(
                    "gradeway.confirmation.request.success",
                    Component.text(rootLiteral),
                    Component.text(jobId)
                )
            )
        }
    }

    registerCopy("backup") {
        permission("gradeway.backup")

        registerCopy("export") {
            permission("gradeway.backup.export")

            handler { context ->
                val audience = audienceProvider.apply(context.sender())

                gradeway.backups.export()
                    .onLeft { error ->
                        if (error is BackupManager.ExportError.FileAlreadyExists) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.backup.export.fileAlreadyExists"
                                )
                            )
                            return@handler
                        }
                        if (error is BackupManager.ExportError.Unexpected) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.backup.export.unexpectedError",
                                    Component.text(error.throwable.message ?: "Unknown")
                                )
                            )
                            return@handler
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

        registerCopy("import") {
            permission("gradeway.backup.import")

            required("file", stringParser())
            optional("wipe", booleanParser())

            handler { context ->
                val audience = audienceProvider.apply(context.sender())

                val file = context.get<String>("file")
                val wipe = context.getOrDefault("wipe", true)

                handleImport(audience, file, wipe)
            }
        }
    }
}
