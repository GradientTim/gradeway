/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.entity.player

import arrow.core.Either
import dev.gradienttim.gradeway.entity.role.RoleEntity
import dev.gradienttim.gradeway.reference.AttributeReference
import dev.gradienttim.gradeway.reference.PermissionReference
import dev.gradienttim.gradeway.reference.PermissionTemplateReference
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
interface PlayerEntity : AttributeReference<PlayerAttributeEntity>,
    PermissionReference<PlayerPermissionEntity>,
    PermissionTemplateReference<PlayerPermissionTemplateEntity> {
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
     * Represents the weight of the player, which can be used to define its relative importance,
     * precedence, or order within a hierarchy or sorting context.
     *
     * A value of `-1` indicates that no weight has been explicitly configured for this player, in
     * which case the player's effective weight is instead derived from its active roles (see
     * [dev.gradienttim.gradeway.services.PlayerService.getEffectiveWeight]). A higher weight might
     * indicate greater importance or precedence, while a lower weight could represent lesser
     * priority.
     */
    var weight: Int

    /**
     * Represents the primary role identifier associated with a player entity.
     *
     * This property holds the unique identifier of a player's primary role within the system,
     * which is represented as an `EntityID` object wrapping a `UUID`. The primary role is used
     * to define the player's main or most significant role in the application's context.
     *
     * A `null` value indicates that no primary role has been assigned to the player.
     * When populated, this value corresponds to the primary key of the associated role
     * in the database schema and facilitates efficient referencing and management of
     * the player's role hierarchy.
     */
    val primaryRoleId: EntityID<UUID>?

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
     * Represents the primary role associated with a player entity.
     *
     * This property links a player to their main role in the system, providing
     * access to attributes, permissions, and metadata related to that role.
     * It can be null if the player does not have a primary role assigned.
     */
    val primaryRole: RoleEntity?

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
     * Represents a collection of attribute entities associated with the player.
     *
     * This property provides a navigable and queryable iterable of [PlayerAttributeEntity] objects
     * that belong to the player. These attributes are key-value pairs that convey additional metadata
     * or properties related to the player, such as configuration details or contextual information.
     *
     * The attributes are stored as entities, enabling database persistence and retrieval,
     * and can be used to extend the player's functionality or to define custom behavior.
     */
    override val attributes: SizedIterable<PlayerAttributeEntity>

    /**
     * Represents the collection of permissions associated with the player.
     *
     * This property retrieves or modifies a set of `PlayerPermissionEntity` objects
     * linked to the current player. Permissions define the actions or access levels
     * granted to the player, shaping its capabilities within the system.
     *
     * The `permissions` property can be used to dynamically manage and query the
     * permissions associated with the player, enabling fine-grained control of access
     * rights in a player-based access control system.
     */
    override val permissions: SizedIterable<PlayerPermissionEntity>

    /**
     * Represents a collection of permission templates associated with the player entity.
     *
     * Each permission template defines a predefined set of permissions that can be
     * applied to the player. This property enables the player to manage its permissions
     * in bulk by referencing these templates. It supports lazy querying, allowing
     * efficient access to the underlying data when needed.
     */
    override val permissionTemplates: SizedIterable<PlayerPermissionTemplateEntity>

    /**
     * Updates the name of this player entity.
     *
     * @param name The new name to assign to the player entity.
     * @return An instance of [Either] containing [PlayerService.SetNameError] if the update fails,
     *         or `true` if the update succeeds.
     */
    fun setName(name: String): Either<PlayerService.SetNameError, Boolean>

    /**
     * Updates the weight of the player.
     *
     * @param weight The new weight to assign to the player.
     * @return An instance of [Either] containing [PlayerService.SetWeightError] if the update fails,
     *         or `true` if the update succeeds.
     */
    fun setWeight(weight: Int): Either<PlayerService.SetWeightError, Boolean>
}
