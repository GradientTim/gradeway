/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.reference

import org.jetbrains.exposed.v1.jdbc.SizedIterable

/**
 * Represents an interface for managing references to permission templates associated with an entity.
 *
 * This interface defines a structure to handle collections of permission templates, where each
 * template specifies a distinct configuration that governs access control or permissions for
 * entities. It provides efficient mechanisms for querying and iterating over these templates,
 * ensuring flexibility in managing them.
 *
 * @param TReference The type of the permission template references managed by this interface.
 */
interface PermissionTemplateReference<TReference> {
    /**
     * Represents a collection of permission templates associated with an entity.
     *
     * Each permission template defines a specific configuration or model that can
     * be used to manage permissions effectively. This property provides support
     * for lazy iteration and efficient querying of the contained templates.
     */
    val permissionTemplates: SizedIterable<TReference>

    /**
     * Sends all cached inserts and updates for this Entity instance to the database.
     *
     * @return `false` if no cached inserts or updates were sent to the database; `true`, otherwise.
     */
    fun flush(): Boolean
}
