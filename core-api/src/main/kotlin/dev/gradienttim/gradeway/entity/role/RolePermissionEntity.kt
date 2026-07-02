/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.entity.role

import dev.gradienttim.gradeway.entity.SharedPermissionEntity
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import java.util.*

/**
 * Represents an entity that links a role to a specific permission within the system.
 *
 * This interface extends the [SharedPermissionEntity] interface, thereby inheriting properties
 * that define a common structure for permission-related entities, such as the permission identifier
 * ([permissionId]), a flag indicating whether the permission is enabled ([isEnabled]), and the
 * associated permission details ([permission]).
 *
 * The [RolePermissionEntity] introduces additional properties specific to the context of roles:
 * - [roleId]: The unique identifier of the role associated with the permission.
 * - [role]: The details of the role entity linked to the permission.
 *
 * This entity is used in role-based access control systems to define which permissions
 * are associated with specific roles, enabling fine-grained management of access rights.
 */
interface RolePermissionEntity : SharedPermissionEntity {
    /**
     * Represents the unique identifier of a role associated with a permission.
     *
     * This property serves as a reference to a specific role entity within the context
     * of role-based access control. It links a permission to a role by storing the
     * corresponding role's primary key.
     *
     * The value is an `EntityID` wrapping a `UUID`, ensuring a globally unique identifier
     * for the role. This property is essential for establishing relationships between
     * roles and permissions in the system.
     */
    val roleId: EntityID<UUID>

    /**
     * Represents the role entity associated with a permission.
     *
     * This property references the [RoleEntity] linked to the current permission context.
     * It provides access to the details of the role, such as its name, attributes,
     * permissions, and hierarchical importance (weight). The [role] property is part
     * of the [RolePermissionEntity] interface, enabling navigation and manipulation
     * of the role associated with a given permission.
     */
    val role: RoleEntity
}
