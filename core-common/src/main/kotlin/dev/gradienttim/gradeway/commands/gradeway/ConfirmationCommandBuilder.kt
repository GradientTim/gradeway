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
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component

internal fun <TSource> ArgumentBuilder<TSource, *>.confirmationBuilder(
    gradeway: CommonGradeway,
    hasPermission: (source: TSource, permission: String) -> Boolean,
    sourceToAudience: (source: TSource) -> Audience,
) {
    literal("confirm") {
        requires { hasPermission(it, "gradeway.confirmJob") }

        string("jobId") {
            execute {
                val audience = sourceToAudience(source)

                val jobId = stringParam("jobId")

                gradeway.confirmations.confirm(jobId)
                    .onLeft { error ->
                        if (error is ConfirmationManager.ConfirmJobError.NotRegistered) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.confirmJob.notRegistered",
                                    Component.text(jobId)
                                )
                            )
                            return@execute
                        }
                        if (error is ConfirmationManager.ConfirmJobError.Unexpected) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.confirmJob.unexpectedError",
                                    Component.text(jobId),
                                    Component.text(error.throwable.message ?: "Unknown")
                                )
                            )
                            return@execute
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
    }

    literal("cancel") {
        requires { hasPermission(it, "gradeway.cancelJob") }

        string("jobId") {
            execute {
                val audience = sourceToAudience(source)

                val jobId = stringParam("jobId")

                gradeway.confirmations.cancel(jobId)
                    .onLeft { error ->
                        if (error is ConfirmationManager.CancelJobError.NotRegistered) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.cancelJob.notRegistered",
                                    Component.text(jobId)
                                )
                            )
                            return@execute
                        }
                        if (error is ConfirmationManager.CancelJobError.Unexpected) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.cancelJob.unexpectedError",
                                    Component.text(jobId),
                                    Component.text(error.throwable.message ?: "Unknown")
                                )
                            )
                            return@execute
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
}
