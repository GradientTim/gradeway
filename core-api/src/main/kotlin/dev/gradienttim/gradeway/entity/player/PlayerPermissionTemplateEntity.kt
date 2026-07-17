/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.entity.player

import dev.gradienttim.gradeway.entity.SharedPermissionTemplateEntity
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import java.util.*

/**
 * Represents the association between a `PlayerEntity` and a `PermissionTemplateEntity`.
 *
 * This entity defines the relationship between a player and a permission template,
 * enabling the usage of preconfigured sets of permissions for a specific player.
 */
interface PlayerPermissionTemplateEntity : SharedPermissionTemplateEntity  {
    /**
     * The unique identifier for a player entity in the `PlayerPermissionTemplateEntity` interface.
     *
     * This property represents a reference to the `PlayerEntity` associated with the specific
     * permission template. It serves as a link between a player and a predefined set of permissions,
     * enabling efficient management of player permissions by leveraging reusable templates.
     *
     * The value is an immutable `EntityID` backed by a `UUID`, ensuring uniqueness within the system.
     */
    val playerId: EntityID<UUID>

    /**
     * Represents the `PlayerEntity` associated with the current permission template.
     *
     * This property provides access to the player entity linked to a specific permission template
     * within the `PlayerPermissionTemplateEntity` interface. It establishes a direct relationship
     * with the `PlayerEntity`, allowing retrieval of player-specific information such as attributes,
     * roles, permissions, and other relevant data.
     *
     * The `player` property acts as a bridge between the player and the permission template,
     * facilitating the application of pre-configured permission sets to the player in a
     * structured and efficient manner.
     */
    val player: PlayerEntity
}
