/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.notification.drivers.ingame

import net.kyori.adventure.text.TranslatableComponent
import java.util.*

/**
 * Connects the `ingame` notification driver to the online players of a platform.
 *
 * Every platform module implements this once and registers it via [IngameNotificationDriver].
 * All functions are called from a task scheduled on the platform's [dev.gradienttim.gradeway.platform.Scheduler].
 *
 * @param TPlayer The platform's online player type.
 */
interface IngameNotificationPlatform<TPlayer> {
    /**
     * Returns the players that are currently online.
     */
    fun onlinePlayers(): Collection<TPlayer>

    /**
     * Returns the unique identifier of the given player.
     */
    fun uniqueId(player: TPlayer): UUID

    /**
     * Returns the name of the online player with the given unique identifier.
     *
     * @return The player's name, or null if no player with this unique identifier is online.
     */
    fun playerName(uniqueId: UUID): String?

    /**
     * Checks whether the given player has the specified permission.
     */
    fun hasPermission(player: TPlayer, permission: String): Boolean

    /**
     * Renders the translatable component in the player's locale and sends it to the player.
     */
    fun sendTranslatedMessage(player: TPlayer, component: TranslatableComponent)
}
