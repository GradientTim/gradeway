/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.entity.group

import dev.gradienttim.gradeway.entity.permission.PermissionTemplateEntity
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import java.util.*

/**
 * Represents the association between a `GroupEntity` and a `PermissionTemplateEntity`.
 *
 * This entity defines the relationship between a group and a permission template,
 * enabling the usage of preconfigured sets of permissions for a specific group.
 */
interface GroupPermissionTemplateEntity {
    /**
     * The unique identifier for a group entity in the `GroupPermissionTemplateEntity` interface.
     *
     * This property represents a reference to the `GroupEntity` associated with the specific
     * permission template. It serves as a link between a group and a predefined set of permissions,
     * enabling efficient management of group permissions by leveraging reusable templates.
     *
     * The value is an immutable `EntityID` backed by a `UUID`, ensuring uniqueness within the system.
     */
    val groupId: EntityID<UUID>

    /**
     * Represents the unique identifier for a permission template within the `GroupPermissionTemplateEntity` interface.
     *
     * This property establishes a reference to the associated `PermissionTemplateEntity` that defines
     * a set of pre-configured permissions. It allows the assignment and management of standardized
     * permissions to a group entity by linking the group with a specific permission template.
     *
     * The value is an immutable `EntityID` backed by a `UUID`, ensuring its uniqueness and reliability
     * in database operations.
     */
    val permissionTemplateId: EntityID<UUID>

    /**
     * Represents the `GroupEntity` associated with the current permission template.
     *
     * This property provides access to the group entity linked to a specific permission template
     * within the `GroupPermissionTemplateEntity` interface. It establishes a direct relationship
     * with the `GroupEntity`, allowing retrieval of group-specific information such as attributes,
     * roles, permissions, and other relevant data.
     *
     * The `group` property acts as a bridge between the group and the permission template,
     * facilitating the application of pre-configured permission sets to the group in a
     * structured and efficient manner.
     */
    val group: GroupEntity

    /**
     * Represents a permission template associated with a specific group.
     *
     * This property links the current group entity to an instance of `PermissionTemplateEntity`,
     * enabling the assignment and management of permission sets for the given group. It can be
     * used to define and enforce the permissions applicable to the group within the associated
     * domain or system context.
     */
    val permissionTemplate: PermissionTemplateEntity
}
