/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.entity.role

import dev.gradienttim.gradeway.entity.group.GroupEntity
import org.jetbrains.exposed.v1.core.dao.id.CompositeID
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import java.util.*

/**
 * Represents an entity that defines the association between a role and a group.
 *
 * This entity forms a many-to-many relationship between `RoleEntity` and `GroupEntity` by
 * linking a specific role to a specific group. It is useful in contexts where roles
 * and groups need to be dynamically associated with permissions, organizational structures,
 * or functional groupings within a system.
 */
interface RoleGroupEntity {
    /**
     * The unique identifier for the role-group relationship entity.
     *
     * This property serves as a composite key that uniquely identifies
     * the association between a specific `RoleEntity` and a `GroupEntity`. It encapsulates
     * the combination of both identifiers, ensuring no duplicate links exist between
     * the same role and group. This identifier plays a critical role in database
     * operations, such as lookups, updates, and deletions, for the corresponding
     * relationship entity.
     */
    val id: EntityID<CompositeID>

    /**
     * Represents the unique identifier of a role associated with a role-group relationship.
     *
     * This property references the primary key of a `RoleEntity` within the system,
     * establishing one side of the association in the many-to-many relationship
     * between roles and groups. It is a foreign key pointing to the `RoleEntity` table
     * and is used to track which specific role participates in the relationship.
     */
    val roleId: EntityID<UUID>

    /**
     * Represents the unique identifier of a group associated with a role-group relationship.
     *
     * This property references the primary key of a `GroupEntity` within the system,
     * establishing one side of the association in the many-to-many relationship
     * between roles and groups. It is a foreign key pointing to the `GroupEntity` table
     * and is used to track which specific group participates in the relationship.
     */
    val groupId: EntityID<UUID>

    /**
     * Represents the associated role entity for this `RoleGroupEntity`.
     *
     * This variable establishes a relationship between the `RoleGroupEntity` and
     * a specific `RoleEntity`, linking the group to its corresponding role within
     * the system. It encapsulates the role information that the group is associated with,
     * enabling access to role-specific properties, attributes, and permissions.
     *
     * By associating a `RoleEntity` with a `RoleGroupEntity`, this variable facilitates
     * the management of role-based access control and group-role mappings.
     */
    val role: RoleEntity

    /**
     * Represents the `GroupEntity` associated with the current role-group relationship.
     *
     * This variable holds a reference to the `GroupEntity` instance that defines the group
     * component of the role-group association. It provides access to group-related attributes,
     * such as its identifier, name, permissions, roles, and metadata.
     *
     * The `group` acts as a critical link within the system that enables the mapping between
     * roles and the groups they belong to. It supports role-based access management, enabling
     * efficient queries and management of group-level configurations and permissions.
     */
    val group: GroupEntity
}
