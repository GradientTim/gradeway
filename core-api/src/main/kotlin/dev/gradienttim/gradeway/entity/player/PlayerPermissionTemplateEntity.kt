/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.entity.player

import dev.gradienttim.gradeway.entity.permission.PermissionTemplateEntity
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import java.util.*

/**
 * Represents the association between a `PlayerEntity` and a `PermissionTemplateEntity`.
 *
 * This entity defines the relationship between a player and a permission template,
 * enabling the usage of preconfigured sets of permissions for a specific player.
 */
interface PlayerPermissionTemplateEntity {
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
     * Represents the unique identifier for a permission template within the `PlayerPermissionTemplateEntity` interface.
     *
     * This property establishes a reference to the associated `PermissionTemplateEntity` that defines
     * a set of pre-configured permissions. It allows the assignment and management of standardized
     * permissions to a player entity by linking the player with a specific permission template.
     *
     * The value is an immutable `EntityID` backed by a `UUID`, ensuring its uniqueness and reliability
     * in database operations.
     */
    val permissionTemplateId: EntityID<UUID>

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

    /**
     * Represents a permission template associated with a specific player.
     *
     * This property links the current player entity to an instance of `PermissionTemplateEntity`,
     * enabling the assignment and management of permission sets for the given player. It can be
     * used to define and enforce the permissions applicable to the player within the associated
     * domain or system context.
     */
    val permissionTemplate: PermissionTemplateEntity
}
