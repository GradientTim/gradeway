/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.reference

import org.jetbrains.exposed.v1.jdbc.SizedIterable

/**
 * Represents an entity or object that maintains a reference to a collection of attributes.
 *
 * This interface provides mechanisms to query and manage attributes associated with the
 * entity. Each attribute represents a specific property or characteristic, enabling
 * efficient handling of attribute data while preserving flexibility for further extension
 * or customization.
 *
 * @param TReference The type of the attribute references managed by this interface.
 */
interface AttributeReference<TReference> {
    /**
     * Represents a collection of attributes associated with the entity. Each attribute defines
     * a specific property or characteristic of the entity and can be queried or manipulated
     * to manage the entity's state effectively. This collection supports lazy queries and provides
     * efficient handling of attribute data.
     */
    val attributes: SizedIterable<TReference>

    /**
     * Sends all cached inserts and updates for this Entity instance to the database.
     *
     * @return `false` if no cached inserts or updates were sent to the database; `true`, otherwise.
     */
    fun flush(): Boolean
}
