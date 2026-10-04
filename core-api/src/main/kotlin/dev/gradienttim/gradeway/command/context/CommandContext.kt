/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.command.context

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TranslatableComponent
import java.util.UUID

/**
 * Represents the context in which a command is executed. This interface enables
 * operations related to the command source and permissions.
 *
 * @param TCommandSource The type representing the source that triggers the command.
 */
interface CommandContext<TCommandSource> {
    /**
     * Converts the provided command source into a UUID.
     *
     * @param source The command source to be converted to a UUID. Represents the entity
     *               or system that triggered the command.
     * @return The UUID associated with the given command source.
     */
    fun sourceToUUID(source: TCommandSource): UUID = EMPTY_UUID

    /**
     * Checks if the given command source has the specified permission.
     *
     * @param source The command source for which the permission is being checked.
     * @param permission The name of the permission to check against the command source.
     * @return True if the command source has the specified permission, false otherwise.
     */
    fun hasPermission(source: TCommandSource, permission: String): Boolean

    /**
     * Sends a message to the specified command source.
     *
     * @param source The command source to which the message will be sent. Represents the entity or system
     *               that executed the command.
     * @param component The message content to be sent, represented as a `Component`.
     */
    fun sendMessage(source: TCommandSource, component: Component)

    /**
     * Sends a translatable message to the specified command source.
     *
     * @param source The command source to which the translatable message will be sent. Represents the entity or
     *               system that executed the command.
     * @param component The translatable message content to be sent, encapsulated as a `TranslatableComponent`.
     */
    fun sendTranslatedMessage(source: TCommandSource, component: TranslatableComponent)

    companion object {
        val EMPTY_UUID = UUID(0, 0)
    }
}
