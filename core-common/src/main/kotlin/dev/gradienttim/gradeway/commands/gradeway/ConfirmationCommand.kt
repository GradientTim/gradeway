/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.commands.gradeway

import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.managers.ConfirmationManager
import net.kyori.adventure.text.Component
import org.incendo.cloud.kotlin.MutableCommandBuilder
import org.incendo.cloud.minecraft.extras.AudienceProvider
import org.incendo.cloud.parser.standard.StringParser.stringParser

internal fun <C : Any> MutableCommandBuilder<C>.registerConfirmationCommand(
    gradeway: CommonGradeway,
    audienceProvider: AudienceProvider<C>,
) {
    registerCopy("confirm") {
        permission("gradeway.confirmJob")

        required("jobId", stringParser())

        handler { context ->
            val audience = audienceProvider.apply(context.sender())

            val jobId = context.get<String>("jobId")

            gradeway.confirmations.confirm(audience, jobId)
                .onLeft { error ->
                    if (error is ConfirmationManager.ConfirmJobError.NotRegistered) {
                        audience.sendMessage(
                            Component.translatable(
                                "gradeway.command.confirmJob.notRegistered",
                                Component.text(jobId)
                            )
                        )
                        return@handler
                    }
                    if (error is ConfirmationManager.ConfirmJobError.WrongSender) {
                        audience.sendMessage(
                            Component.translatable(
                                "gradeway.command.confirmJob.wrongSender",
                                Component.text(jobId)
                            )
                        )
                        return@handler
                    }
                    if (error is ConfirmationManager.ConfirmJobError.Unexpected) {
                        audience.sendMessage(
                            Component.translatable(
                                "gradeway.command.confirmJob.unexpectedError",
                                Component.text(jobId),
                                Component.text(error.throwable.message ?: "Unknown")
                            )
                        )
                        return@handler
                    }
                }
                .onRight {
                    audience.sendMessage(
                        Component.translatable(
                            "gradeway.command.confirmJob.success",
                            Component.text(jobId)
                        )
                    )
                }
        }
    }

    registerCopy("cancel") {
        permission("gradeway.cancelJob")

        required("jobId", stringParser())

        handler { context ->
            val audience = audienceProvider.apply(context.sender())

            val jobId = context.get<String>("jobId")

            gradeway.confirmations.cancel(audience, jobId)
                .onLeft { error ->
                    if (error is ConfirmationManager.CancelJobError.NotRegistered) {
                        audience.sendMessage(
                            Component.translatable(
                                "gradeway.command.cancelJob.notRegistered",
                                Component.text(jobId)
                            )
                        )
                        return@handler
                    }
                    if (error is ConfirmationManager.CancelJobError.WrongSender) {
                        audience.sendMessage(
                            Component.translatable(
                                "gradeway.command.cancelJob.wrongSender",
                                Component.text(jobId)
                            )
                        )
                        return@handler
                    }
                    if (error is ConfirmationManager.CancelJobError.Unexpected) {
                        audience.sendMessage(
                            Component.translatable(
                                "gradeway.command.cancelJob.unexpectedError",
                                Component.text(jobId),
                                Component.text(error.throwable.message ?: "Unknown")
                            )
                        )
                        return@handler
                    }
                }
                .onRight {
                    audience.sendMessage(
                        Component.translatable(
                            "gradeway.command.cancelJob.success",
                            Component.text(jobId)
                        )
                    )
                }
        }
    }
}
