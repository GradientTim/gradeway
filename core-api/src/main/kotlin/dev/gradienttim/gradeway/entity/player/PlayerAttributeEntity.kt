/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.entity.player

import dev.gradienttim.gradeway.entity.SharedAttributeEntity
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import java.util.*

/**
 * Represents an attribute entity associated with a specific player, extending the shared attribute model.
 * This interface ties an attribute to a particular player entity, enabling the storage of player-specific
 * metadata or configuration details in the form of key-value pairs.
 *
 * The `PlayerAttributeEntity` interface inherits functionality from `SharedAttributeEntity`, including
 * properties for the attribute's unique identifier, key, type, and value, as well as timestamps for creation
 * and last update. Additionally, it provides a link to the player entity to which the attribute belongs.
 *
 * This interface is typically used in systems where players have unique, customizable attributes
 * that are stored and retrieved dynamically. The relationship between the attribute and the player
 * is managed via the `playerId` and `player` properties.
 */
interface PlayerAttributeEntity : SharedAttributeEntity {
    /**
     * Represents the unique identifier of the player associated with the attribute.
     *
     * This property links the attribute entity to a specific player entity, enabling
     * the association of player-specific metadata or configurations. The value is
     * stored as an `EntityID<UUID>` to ensure both uniqueness and referential integrity
     * within the database schema.
     */
    val playerId: EntityID<UUID>

    /**
     * Represents the player entity associated with the current attribute entity.
     *
     * The `player` property provides a reference to the `PlayerEntity` instance that is linked
     * to this attribute. This association enables access to the broader context of the player,
     * including their attributes, permissions, roles, and other metadata.
     *
     * This property is particularly useful in systems where an attribute needs to maintain
     * knowledge of its owning player entity to enable operations or queries that involve
     * both the attribute and the player as a whole.
     */
    val player: PlayerEntity
}
