/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.entity

/**
 * Represents an entity that contains a set of permissions. This interface provides mechanisms
 * for managing and persisting permission states as key-value pairs, where the key is the permission name
 * and the value indicates whether the permission is granted.
 */
interface PermissionEntity {
    /**
     * A map containing the key-value pairs representing permission states.
     *
     * The key is a string that identifies the permission, while the value is a boolean indicating whether the permission is granted (`true`) or revoked (`false`).
     * This property is typically used to store and manage the permission configuration for an entity, such as a role or a player.
     */
    var permissions: Map<String, Boolean>

    /**
     * Sends all cached inserts and updates for this Entity instance to the database.
     *
     * @return `false` if no cached inserts or updates were sent to the database; `true`, otherwise.
     */
    fun flush(): Boolean
}
