/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.entity.role

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import java.util.UUID

/**
 * Represents a parent-child relationship between two role entities within the system.
 *
 * This entity models a hierarchical structure, where a role can have another role
 * as its parent or child. This hierarchy might be used to represent inheritance,
 * dependencies, or grouping within a role-based access control system.
 */
interface RoleParentEntity {
    /**
     * Represents the identifier of the parent role in a hierarchical relationship.
     *
     * This property references the unique identifier (UUID) of the parent `RoleEntity`
     * within a parent-child structure. It is used to model dependencies or inheritance
     * in a role-based access control system, where the parent role can pass down
     * attributes or permissions to its child roles.
     */
    val parentId: EntityID<UUID>

    /**
     * Represents the identifier of the child's role in a hierarchical relationship.
     *
     * This property references the unique identifier (UUID) of the child `RoleEntity`
     * within a parent-child structure. It is used to model dependencies or inheritance
     * in a role-based access control system, where the child role can inherit attributes
     * or permissions from its parent role.
     */
    val childId: EntityID<UUID>

    /**
     * Represents the parent's role in a hierarchical relationship between roles.
     *
     * The `parent` property refers to the `RoleEntity` that serves as the parent
     * in a parent-child structure within a role-based access control system.
     * This parent role can pass down its attributes, permissions, or hierarchical
     * context to its child roles. It is used to model role dependencies, grouping,
     * or inheritance in the system.
     */
    val parent: RoleEntity

    /**
     * Represents the child's role in a hierarchical relationship between roles.
     *
     * The `child` property refers to the `RoleEntity` that serves as the child
     * in a parent-child structure within a role-based access control system.
     * This child role can inherit attributes, permissions, or hierarchical context
     * from its parent role. It is used to model role dependencies, grouping,
     * or inheritance in the system.
     */
    val child: RoleEntity
}
