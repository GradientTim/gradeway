/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.entity.role

import dev.gradienttim.gradeway.entity.SharedAttributeEntity
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import java.util.*

/**
 * Represents an entity for linking attributes to roles.
 *
 * This interface extends the functionality of [SharedAttributeEntity], introducing
 * properties specific to roles. It provides the ability to associate a role's
 * identifier and corresponding role entity with an attribute, enabling a connection
 * between roles and their metadata properties.
 *
 * The `RoleAttributeEntity` plays a critical role in establishing a relationship
 * between attributes and roles within a system and enhances customization and extensibility
 * through the attachment of key-value pair information to role entities.
 */
interface RoleAttributeEntity : SharedAttributeEntity {
    /**
     * A property representing the unique identifier for a role associated with an attribute.
     *
     * This identifier is a foreign key linking the `RoleAttributeEntity` to a specific `RoleEntity`.
     * It facilitates the establishment of a relationship between an attribute and its respective role,
     * enabling metadata and configurational linkage in the system.
     *
     * The `roleId` provides a direct reference to the associated role, ensuring that attributes
     * can be tied to specific roles for extensibility and precise association of properties.
     */
    val roleId: EntityID<UUID>

    /**
     * Represents the role entity associated with a specific attribute.
     *
     * This val provides a navigable reference to the corresponding `RoleEntity` for the current
     * instance of `RoleAttributeEntity`. It establishes a link between the attribute and the
     * role with which it is associated, enabling retrieval of details or metadata about the role.
     *
     * The `role` property is pivotal in connecting attributes to roles, facilitating role-based
     * configurations and enabling the extension of attributes with role-specific context or
     * functionality in the system.
     */
    val role: RoleEntity
}
