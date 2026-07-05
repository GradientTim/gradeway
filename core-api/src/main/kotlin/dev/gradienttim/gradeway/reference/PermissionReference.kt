/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.reference

import org.jetbrains.exposed.v1.jdbc.SizedIterable

/**
 * Represents an entity or object that maintains a reference to a collection of permissions.
 *
 * This interface provides mechanisms to query and manage permissions associated with the
 * entity. Each permission represents a specific property or characteristic, enabling
 * efficient handling of permission data while preserving flexibility for further extension
 * or customization.
 *
 * @param TReference The type of the permission references managed by this interface.
 */
interface PermissionReference<TReference> {
    /**
     * Represents a collection of permissions associated with the entity. Each permission
     * defines a specific action or access level that can be granted or denied. This collection
     * can be queried or manipulated to manage the entity's permission states effectively.
     */
    val permissions: SizedIterable<TReference>
}
