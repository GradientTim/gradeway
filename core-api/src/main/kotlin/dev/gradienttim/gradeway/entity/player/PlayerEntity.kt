/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.entity.player

import arrow.core.Either
import dev.gradienttim.gradeway.attribute.Attribute
import dev.gradienttim.gradeway.entity.AttributeEntity
import dev.gradienttim.gradeway.entity.PermissionEntity
import dev.gradienttim.gradeway.services.PlayerService
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.jdbc.SizedIterable
import java.time.Instant
import java.util.*

/**
 * Represents a player entity that combines attributes and permissions and provides mechanisms
 * for managing and persisting both. This interface is an extension of `AttributeEntity` and `PermissionEntity`,
 * allowing it to represent an entity with a unique ID, name, creation timestamp, and update timestamp.
 * It also extends the shared services `SharedAttributeService` and `SharedPermissionService` specific
 * to `PlayerEntity` types.
 */
interface PlayerEntity : AttributeEntity, PermissionEntity {
    /**
     * The unique identifier for the entity. This value is immutable and is used to distinctly identify
     * the entity instance across systems or databases. It is typically generated as a UUID, ensuring
     * a high degree of uniqueness and collision resistance.
     */
    val id: EntityID<UUID>

    /**
     * Represents the name of the player entity. This is a mutable property that typically stores the
     * unique display name or identifier associated with a `PlayerEntity`.
     */
    var name: String

    /**
     * A collection of attributes associated with the player entity.
     *
     * Each attribute is represented as an instance of `Attribute<*>`, allowing for
     * a flexible and type-safe way to store and manage diverse attributes for the player.
     *
     * The set of attributes can be modified to add or remove attributes as needed,
     * enabling dynamic and customizable configurations for different player entities.
     *
     * Overrides the `attributes` property from the `AttributeEntity` interface.
     */
    override var attributes: Set<Attribute<*>>

    /**
     * A map containing key-value pairs representing the permission states associated with the player.
     *
     * The key is a string that identifies the specific permission, and the value is a boolean indicating
     * whether the permission is granted (`true`) or denied (`false`). This property allows managing the
     * permissions specific to the player entity, providing fine-grained control over access levels
     * and capabilities.
     */
    override var permissions: Map<String, Boolean>

    /**
     * The timestamp representing when this player entity was created.
     *
     * This property is used to record the exact moment the entity was instantiated
     * and stored, providing a temporal reference for creation events.
     */
    val createdAt: Instant

    /**
     * The timestamp indicating the last time the entity was updated.
     *
     * This property is automatically maintained to record the date and time of the most
     * recent modification to the entity's state. It is typically used for auditing and
     * synchronization purposes.
     */
    val updatedAt: Instant

    /**
     * Represents the collection of `PlayerRoleEntity` instances associated with a `PlayerEntity`.
     *
     * Each `PlayerRoleEntity` in this collection links the player to a specific role,
     * providing information about the relationship state, such as whether the role is active (`isPaused`)
     * or primary (`isPrimary`), along with temporal metadata (e.g., `untilAt`, `pausedAt`, `createdAt`, `updatedAt`).
     *
     * This property serves as a reference to list or manipulate the roles assigned to a player entity.
     */
    val roles: SizedIterable<PlayerRoleEntity>

    /**
     * Updates the name of this player entity.
     *
     * @param name The new name to assign to the player entity.
     * @return An instance of [Either] containing [PlayerService.SetNameError] if the update fails,
     *         or `true` if the update succeeds.
     */
    fun setName(name: String): Either<PlayerService.SetNameError, Boolean>
}
