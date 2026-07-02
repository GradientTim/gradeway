/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.entity.role

import dev.gradienttim.gradeway.entity.permission.PermissionTemplateEntity
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import java.util.UUID

/**
 * Represents the association between a role and a permission template in the system.
 *
 * This entity defines a many-to-many relationship between `RoleEntity` and `PermissionTemplateEntity`,
 * allowing for the assignment of predefined sets of permissions (templates) to specific roles.
 * It facilitates the management of permissions by grouping them into reusable templates and associating
 * those templates with roles.
 */
interface RolePermissionTemplateEntity {
    /**
     * Represents the unique identifier of the associated role in the many-to-many relationship
     * between `RoleEntity` and `PermissionTemplateEntity`.
     *
     * This property links the `RolePermissionTemplateEntity` to a specific `RoleEntity`,
     * providing the ability to associate roles with predefined permission templates.
     * The identifier is stored as an `EntityID` type with a UUID for uniqueness.
     */
    val roleId: EntityID<UUID>

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
     * Represents the role associated with a specific relationship in the context of
     * the `RolePermissionTemplateEntity`.
     *
     * This property provides access to the `RoleEntity` instance that is tied to a
     * many-to-many relationship between roles and permission templates. It enables
     * navigating from the relationship entity to the corresponding role.
     *
     * The role defines attributes such as its name, weight, associated permissions,
     * and timestamps for its creation and last update, facilitating role management
     * within the system.
     */
    val role: RoleEntity

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
