/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.entity.player

import dev.gradienttim.gradeway.entity.role.RoleEntity
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import java.time.Instant
import java.util.UUID

/**
 * Represents the association between a player and a role, defining their relationship and state.
 *
 * This interface provides properties to manage and reference the link between a `PlayerEntity` and a
 * `RoleEntity`, including the role's primary status, paused state, and associated timestamps.
 * It also allows accessing the relevant `PlayerEntity` and `RoleEntity` instances for further operations
 * and contextual information.
 */
interface PlayerRoleEntity {
    /**
     * Represents the unique identifier of a player associated with a specific role in the `PlayerRoleEntity` context.
     *
     * This UUID-based identifier establishes a relationship between the `PlayerEntity` and its roles, enabling
     * efficient referencing and management of role assignments. The identifier corresponds to the primary key
     * of the `PlayerEntity` within the database.
     */
    val playerId: EntityID<UUID>

    /**
     * Represents the unique identifier of a role associated with a player in the `PlayerRoleEntity` context.
     *
     * This UUID-based identifier establishes a relationship between the `RoleEntity` and the `PlayerEntity`,
     * enabling efficient referencing and management of role assignments. The identifier corresponds to the
     * primary key of the `RoleEntity` within the database.
     */
    val roleId: EntityID<UUID>

    /**
     * Indicates whether the role associated with a player is currently paused.
     *
     * This property is used to track the active state of the role. A `true` value signifies that the role
     * is paused and not actively used in its current context, while a `false` value implies that the
     * role is active and operational. This can be helpful for scenarios where temporary suspension of
     * certain roles is required without removing their association with the player.
     *
     * The paused state can be further contextualized with timestamps such as `pausedAt` to provide
     * additional details regarding when the pause occurred or `untilAt` to specify when the pause state
     * ends, if applicable.
     */
    val isPaused: Boolean

    /**
     * Indicates whether the associated role is the primary role for the player.
     *
     * This property is used to denote the primary status of a player's role, which
     * may define the primary responsibilities or privileges assigned to the player
     * within a specific context. Roles where `isPrimary` is `true` are typically
     * used to identify the main role of the player in relation to the entity.
     */
    val isPrimary: Boolean

    /**
     * Represents the timestamp until which a specific player role is considered active.
     *
     * This property indicates the expiration time of a role assigned to a player.
     * If the value is `null`, it signifies that the role does not have a defined end time
     * and is considered active indefinitely unless explicitly revoked or paused.
     * When a non-null value is provided, it specifies the exact point in time
     * after which the role is no longer active.
     */
    val untilAt: Instant?

    /**
     * Timestamp indicating when the associated role was paused.
     *
     * This property is used to record the moment when the role was set to a paused state.
     * A `null` value indicates that the role is currently active and has not been paused.
     *
     * It serves as part of the temporal metadata associated with the player's roles, enabling
     * tracking and management of role state changes over time.
     */
    val pausedAt: Instant?

    /**
     * The timestamp representing when this role assignment was created.
     *
     * This property records the exact time at which the `PlayerRoleEntity` instance
     * was created and persisted. It serves as a reference for identifying when the
     * relationship between a player and a role was established.
     */
    val createdAt: Instant

    /**
     * The timestamp indicating the last time the entity was updated.
     *
     * This property is used to track the most recent modification of the entity,
     * ensuring accurate audit trails and providing a reliable way to synchronize
     * changes across systems. It is automatically updated whenever the entity's
     * state is modified.
     */
    val updatedAt: Instant

    /**
     * Represents a specific `PlayerEntity` associated with this `PlayerRoleEntity`.
     *
     * This property provides a reference to the player entity linked to the current player-role mapping,
     * allowing access to detail about the player, such as its attributes, permissions, name, and other metadata.
     *
     * It is used to establish and manage the relationship between a player and its assigned roles within the
     * system. The `player` variable is immutable and ensures a consistent association with the relevant
     * `PlayerEntity` instance.
     */
    val player: PlayerEntity

    /**
     * Represents the role assigned to a player within a `PlayerRoleEntity`.
     *
     * This property establishes a reference to a specific `RoleEntity` instance, encapsulating
     * attributes such as the role's unique identifier, name, weight, permissions, and associated
     * metadata (e.g., creation and update timestamps). It is utilized to define the role-related
     * state and functionality linked to the player.
     */
    val role: RoleEntity
}
