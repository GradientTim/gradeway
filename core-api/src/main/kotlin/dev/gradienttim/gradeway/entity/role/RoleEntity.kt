/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.entity.role

import arrow.core.Either
import dev.gradienttim.gradeway.reference.AttributeReference
import dev.gradienttim.gradeway.reference.PermissionReference
import dev.gradienttim.gradeway.reference.PermissionTemplateReference
import dev.gradienttim.gradeway.services.RoleService
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.jdbc.SizedIterable
import java.time.Instant
import java.util.*

/**
 * Represents a role entity that combines functionalities of both `AttributeEntity` and `PermissionEntity`.
 * A `RoleEntity` has an identifier, a name, a weight, attributes, permissions,
 * and timestamps for when it was created and last updated. It also provides
 * mechanisms to modify its name and weight.
 */
interface RoleEntity : AttributeReference<RoleAttributeEntity>, PermissionReference<RolePermissionEntity>,
    PermissionTemplateReference<RolePermissionTemplateEntity> {
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
     * Represents a collection of attribute entities associated with the role.
     *
     * This property provides a navigable and queryable iterable of [RoleAttributeEntity] objects
     * that belong to the role. These attributes are key-value pairs that convey additional metadata
     * or properties related to the role, such as configuration details or contextual information.
     *
     * The attributes are stored as entities, enabling database persistence and retrieval,
     * and can be used to extend the role's functionality or to define custom behavior.
     */
    override val attributes: SizedIterable<RoleAttributeEntity>

    /**
     * Represents the collection of permissions associated with the role.
     *
     * This property retrieves or modifies a set of `RolePermissionEntity` objects
     * linked to the current role. Permissions define the actions or access levels
     * granted to the role, shaping its capabilities within the system.
     *
     * The `permissions` property can be used to dynamically manage and query the
     * permissions associated with the role, enabling fine-grained control of access
     * rights in a role-based access control system.
     */
    override val permissions: SizedIterable<RolePermissionEntity>

    /**
     * Represents a collection of permission templates associated with the role entity.
     *
     * Each permission template defines a predefined set of permissions that can be
     * applied to the role. This property enables the role to manage its permissions
     * in bulk by referencing these templates. It supports lazy querying, allowing
     * efficient access to the underlying data when needed.
     */
    override val permissionTemplates: SizedIterable<RolePermissionTemplateEntity>

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
