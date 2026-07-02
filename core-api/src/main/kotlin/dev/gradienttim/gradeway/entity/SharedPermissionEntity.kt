/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.entity

import dev.gradienttim.gradeway.entity.permission.PermissionEntity
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import java.util.*

/**
 * Represents a shared permission entity that associates a specific permission with its state
 * of being enabled or disabled. This interface defines the structure for entities that manage
 * permissions in a shared or distributed system context.
 */
interface SharedPermissionEntity {
    /**
     * Represents the unique identifier for a permission in the context of a shared permission entity.
     *
     * This identifier is of type `EntityID<UUID>`, which ensures global uniqueness and serves as a
     * reference to specific permissions within the system. It is a key element in managing permission
     * associations and facilitates integration with the database for querying, updating, or managing
     * permission-related data.
     */
    val permissionId: EntityID<UUID>

    /**
     * Indicates whether the associated permission is currently enabled or disabled.
     *
     * When set to `true`, this property signifies that the permission is active and available
     * for use within the system. Conversely, a value of `false` implies that the permission
     * is disabled and not functional.
     *
     * This property is typically used to manage access control and dynamically alter the
     * enabled state of permissions in shared or distributed system contexts.
     */
    var isEnabled: Boolean

    /**
     * Represents the permission entity associated with a shared permission entity.
     *
     * This property provides access to the `PermissionEntity` instance linked to the shared
     * permission entity. It encapsulates details about the permission, including its unique
     * identifier, value, matching strategy, and validation mechanisms. This association
     * enables interaction with the specific permission managed by the shared entity.
     */
    val permission: PermissionEntity
}
