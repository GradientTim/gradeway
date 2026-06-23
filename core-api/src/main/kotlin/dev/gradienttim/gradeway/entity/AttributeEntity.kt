/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.entity

import dev.gradienttim.gradeway.attribute.Attribute

/**
 * Represents an entity that contains a set of attributes. This interface provides a mechanism
 * to manage and persist the state of its attributes to a database.
 */
interface AttributeEntity {
    /**
     * A collection of attributes associated with the entity. Each attribute represents
     * a piece of data or a property tied to the entity. This set is used to define
     * and manipulate the properties of the entity in a structured manner.
     */
    var attributes: Set<Attribute<*>>

    /**
     * Sends all cached inserts and updates for this Entity instance to the database.
     *
     * @return `false` if no cached inserts or updates were sent to the database; `true`, otherwise.
     */
    fun flush(): Boolean
}
