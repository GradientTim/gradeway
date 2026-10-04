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
import dev.gradienttim.gradeway.managers.ConfirmationManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.translation.Argument

internal fun <TCommandSource> ArgumentBuilder<TCommandSource, *>.confirmationCommand(
    gradeway: CommonGradeway<*>,
    commandContext: CommandContext<TCommandSource>,
) {
    literal("confirm") {
        requires { commandContext.hasPermission(it, "gradeway.confirmJob") }

        string("jobId") {
            execute {
                val jobId = stringParam("jobId")

                gradeway.confirmations.confirm(commandContext.sourceToUUID(source), jobId)
                    .onLeft { error ->
                        if (error is ConfirmationManager.ConfirmJobError.NotRegistered) {
                            commandContext.sendTranslatedMessage(
                                source,
                                Component.translatable(
                                    "gradeway.command.confirmJob.notRegistered",
                                    Argument.string("job", jobId)
                                )
                            )
                            return@execute
                        }
                        if (error is ConfirmationManager.ConfirmJobError.WrongSender) {
                            commandContext.sendTranslatedMessage(
                                source,
                                Component.translatable(
                                    "gradeway.command.confirmJob.wrongSender",
                                    Argument.string("job", jobId)
                                )
                            )
                            return@execute
                        }
                        if (error is ConfirmationManager.ConfirmJobError.Unexpected) {
                            commandContext.sendTranslatedMessage(
                                source,
                                Component.translatable(
                                    "gradeway.command.confirmJob.unexpectedError",
                                    Argument.string("job", jobId),
                                    Argument.string("error", error.throwable.message ?: "Unknown")
                                )
                            )
                            return@execute
                        }
                    }
                    .onRight {
                        commandContext.sendTranslatedMessage(
                            source,
                            Component.translatable(
                                "gradeway.command.confirmJob.success",
                                Argument.string("job", jobId)
                            )
                        )
                    }
            }
        }
    }

    literal("cancel") {
        requires { commandContext.hasPermission(it, "gradeway.cancelJob") }

        string("jobId") {
            execute {
                val jobId = stringParam("jobId")

                gradeway.confirmations.cancel(commandContext.sourceToUUID(source), jobId)
                    .onLeft { error ->
                        if (error is ConfirmationManager.CancelJobError.NotRegistered) {
                            commandContext.sendTranslatedMessage(
                                source,
                                Component.translatable(
                                    "gradeway.command.cancelJob.notRegistered",
                                    Argument.string("job", jobId)
                                )
                            )
                            return@execute
                        }
                        if (error is ConfirmationManager.CancelJobError.WrongSender) {
                            commandContext.sendTranslatedMessage(
                                source,
                                Component.translatable(
                                    "gradeway.command.cancelJob.wrongSender",
                                    Argument.string("job", jobId)
                                )
                            )
                            return@execute
                        }
                        if (error is ConfirmationManager.CancelJobError.Unexpected) {
                            commandContext.sendTranslatedMessage(
                                source,
                                Component.translatable(
                                    "gradeway.command.cancelJob.unexpectedError",
                                    Argument.string("job", jobId),
                                    Argument.string("error", error.throwable.message ?: "Unknown")
                                )
                            )
                            return@execute
                        }
                    }
                    .onRight {
                        commandContext.sendTranslatedMessage(
                            source,
                            Component.translatable(
                                "gradeway.command.cancelJob.success",
                                Argument.string("job", jobId)
                            )
                        )
                    }
            }
        }
    }
}
