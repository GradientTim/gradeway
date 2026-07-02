/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.entity.player

import dev.gradienttim.gradeway.entity.SharedPermissionEntity
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import java.util.*

/**
 * Represents a player-specific permission entity within the system.
 *
 * `PlayerPermissionEntity` provides an interface for defining permissions tied to a specific player.
 * It extends the `SharedPermissionEntity` interface, inheriting common permission fields, and adds
 * player-specific associations and properties. This interface serves as a foundational contract
 * for managing permissions in a player-centric manner.
 */
interface PlayerPermissionEntity: SharedPermissionEntity {
    /**
     * The unique identifier for the player entity.
     *
     * This property represents a UUID-backed entity ID that uniquely identifies a player
     * within the system. It is used to establish relationships with other entities,
     * such as permissions or attributes, and guarantees uniqueness across all instances.
     */
    val playerId: EntityID<UUID>

    /**
     * Represents the associated `PlayerEntity` for the current entity.
     *
     * This property provides a link to a `PlayerEntity` instance and is used to
     * establish a relationship between the implementing entity and a specific player.
     * The `PlayerEntity` serves as a central concept encompassing attributes, permissions,
     * and other player-related metadata within the system.
     */
    val player: PlayerEntity
}
