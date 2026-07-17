/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.entity

import dev.gradienttim.gradeway.entity.permission.PermissionTemplateEntity
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import java.util.*

/**
 * Represents an entity that establishes a relationship between `RoleEntity` and
 * `PermissionTemplateEntity`, facilitating the association of roles with predefined
 * sets of permissions.
 */
interface SharedPermissionTemplateEntity {
    /**
     * Represents the unique identifier of the associated permission template
     * in the many-to-many relationship between `RoleEntity` and `PermissionTemplateEntity`.
     *
     * This property links the `RolePermissionTemplateEntity` to a specific `PermissionTemplateEntity`,
     * allowing roles to reference predefined sets of permissions as templates.
     * The identifier is stored as an `EntityID` type with a UUID for uniqueness.
     */
    val permissionTemplateId: EntityID<UUID>

    /**
     * Represents a permission template associated with a specific role.
     *
     * This variable holds an instance of `PermissionTemplateEntity` that defines
     * a set of permissions and their assigned scope. It is used to manage and
     * configure role-specific permission templates within the context of the
     * `RolePermissionTemplateEntity` class.
     */
    val permissionTemplate: PermissionTemplateEntity
}
