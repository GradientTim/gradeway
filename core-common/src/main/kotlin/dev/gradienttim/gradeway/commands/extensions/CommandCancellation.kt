/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.commands.extensions

import dev.gradienttim.gradeway.command.context.CommandContext
import dev.gradienttim.gradeway.event.EventCancelledError
import net.kyori.adventure.text.Component

/**
 * Tells [source] that a listener canceled the operation when [error] is an [EventCancelledError].
 *
 * @return Whether [error] was a cancellation and a message was sent.
 */
internal fun <TCommandSource> CommandContext<TCommandSource>.sendIfCancelled(
    source: TCommandSource,
    error: Any,
): Boolean {
    if (error !is EventCancelledError) {
        return false
    }
    sendTranslatedMessage(source, Component.translatable("gradeway.command.cancelled"))
    return true
}
