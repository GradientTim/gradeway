/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.entity.group

import dev.gradienttim.gradeway.entity.SharedPermissionTemplateEntity
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import java.util.*

/**
 * Represents the association between a `GroupEntity` and a `PermissionTemplateEntity`.
 *
 * This entity defines the relationship between a group and a permission template,
 * enabling the usage of preconfigured sets of permissions for a specific group.
 */
interface GroupPermissionTemplateEntity : SharedPermissionTemplateEntity  {
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
}
