/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.entity.role

import arrow.core.Either
import dev.gradienttim.gradeway.attribute.Attribute
import dev.gradienttim.gradeway.entity.AttributeEntity
import dev.gradienttim.gradeway.entity.PermissionEntity
import dev.gradienttim.gradeway.services.RoleService
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import java.time.Instant
import java.util.*

/**
 * Represents a role entity that combines functionalities of both `AttributeEntity` and `PermissionEntity`.
 * A `RoleEntity` has an identifier, a name, a weight, attributes, permissions,
 * and timestamps for when it was created and last updated. It also provides
 * mechanisms to modify its name and weight.
 */
interface RoleEntity : AttributeEntity, PermissionEntity {
    /**
     * The unique identifier for the entity. This value is immutable and is used to distinctly identify
     * the entity instance across systems or databases. It is typically generated as a UUID, ensuring
     * a high degree of uniqueness and collision resistance.
     */
    val id: EntityID<UUID>

    /**
     * Represents the name of the role entity. This is a mutable property that typically stores the
     * unique display name or identifier associated with a `RoleEntity`.
     */
    var name: String

    /**
     * Represents the weight of the role, which can be used to define its relative importance,
     * precedence, or order within a hierarchy or sorting context.
     *
     * A higher weight might indicate greater importance or precedence, while a lower weight
     * could represent lesser priority. This attribute could be used in various contexts,
     * such as defining the order of roles in a list or determining the overriding precedence
     * in a role-based access control system.
     */
    var weight: Int

    /**
     * A collection of attributes associated with the role entity.
     *
     * Each attribute is represented as an instance of `Attribute<*>`, allowing for
     * a flexible and type-safe way to store and manage diverse attributes for the role.
     *
     * The set of attributes can be modified to add or remove attributes as needed,
     * enabling dynamic and customizable configurations for different role entities.
     *
     * Overrides the `attributes` property from the `AttributeEntity` interface.
     */
    override var attributes: Set<Attribute<*>>

    /**
     * A map containing key-value pairs representing the permission states associated with the role.
     *
     * The key is a string that identifies the specific permission, and the value is a boolean indicating
     * whether the permission is granted (`true`) or denied (`false`). This property allows managing the
     * permissions specific to the role entity, providing fine-grained control over access levels
     * and capabilities.
     */
    override var permissions: Map<String, Boolean>

    /**
     * The timestamp representing when the role entity was created.
     *
     * This property holds the point in time at which the `RoleEntity` instance
     * was initially created in the system. It is used for tracking the creation
     * date and time of the role entity and can be used for auditing or
     * chronological sorting purposes.
     */
    val createdAt: Instant

    /**
     * The timestamp indicating the last time the role entity was updated.
     *
     * This property represents the point in time when the `RoleEntity` instance
     * was last modified. It is used for tracking the most recent update date
     * and time of the entity and can assist in auditing, synchronization, or
     * determining outdated data.
     */
    val updatedAt: Instant

    /**
     * Updates the name of the role.
     *
     * @param name The new name to assign to the role.
     * @return An instance of [Either] containing [RoleService.SetNameError] if the update fails,
     *         or `true` if the update is successful.
     */
    fun setName(name: String): Either<RoleService.SetNameError, Boolean>

    /**
     * Updates the weight of the role.
     *
     * @param weight The new weight to assign to the role.
     * @return An instance of [Either] containing [RoleService.SetWeightError] if the update fails,
     *         or `true` if the update succeeds.
     */
    fun setWeight(weight: Int): Either<RoleService.SetWeightError, Boolean>
}
