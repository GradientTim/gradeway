/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.entity.permission

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import java.util.*

/**
 * Defines the association between a permission and a permission template.
 *
 * This entity represents a linkage connecting a specific permission
 * (`PermissionEntity`) to a permission template (`PermissionTemplateEntity`),
 * enabling the definition of permission sets for templates.
 * It allows managing multiple permissions under a unified template structure.
 */
interface PermissionTemplatePermissionEntity {
    /**
     * Unique identifier for a permission template entity.
     *
     * This property links the permission to a specific template entity within the system.
     * It serves as a foreign key reference to the associated `PermissionTemplateEntity`.
     */
    val templateId: EntityID<UUID>

    /**
     * Unique identifier for a permission entity within a permission-template relationship.
     *
     * This property serves as a foreign key reference to the associated `PermissionEntity`
     * in the context of its linkage to a permission template. It is used to establish
     * and manage the association between a specific permission and its corresponding
     * template definition.
     */
    val permissionId: EntityID<UUID>

    /**
     * Represents the permission template entity associated with the current permission-template relationship.
     *
     * This property provides access to the `PermissionTemplateEntity` linked to the given permission,
     * enabling the retrieval of detailed information about the template's configuration, assigned scope,
     * and associated permissions.
     */
    val template: PermissionTemplateEntity

    /**
     * Represents the permission entity associated with the current permission-template relationship.
     *
     * This property provides access to the `PermissionEntity` linked to a specific permission-template association.
     * It facilitates retrieving detailed information about the permission's unique identifier, validation type,
     * matching strategy, and associated value. Additionally, it enables validation of permission strings
     * according to the defined matching rules in the `PermissionEntity`.
     */
    val permission: PermissionEntity
}
