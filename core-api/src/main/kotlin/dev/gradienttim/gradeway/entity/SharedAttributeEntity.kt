/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.entity

import dev.gradienttim.gradeway.attribute.Attribute
import net.kyori.adventure.key.Key
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import java.time.Instant
import java.util.*

/**
 * Represents a shared attribute entity that defines a key-value pair with associated metadata
 * for extensible entities. This interface is designed to provide a flexible mechanism for
 * storing and managing attributes in a structured manner.
 */
interface SharedAttributeEntity {
    /**
     * Represents the unique identifier for an entity implementing the
     * `SharedAttributeEntity` interface.
     *
     * This identifier is of type `EntityID<UUID>`, which uniquely
     * distinguishes each shared attribute entity. It is primarily used
     * for identifying and managing entities in a database context.
     */
    val id: EntityID<UUID>

    /**
     * Represents the unique identifier for an attribute within a shared attribute entity.
     *
     * This key serves as a reference to distinguish one attribute from another, enabling
     * operations such as retrieval, update, deletion, and existence checks for specific attributes
     * associated with an entity. It ensures consistency and uniqueness across attribute management
     * workflows.
     */
    val key: Key

    /**
     * Represents the type key of the shared attribute entity.
     *
     * This property is used to categorize or define the type of the shared attribute, which can
     * be helpful in managing and differentiating attributes within an entity. The type key is
     * a fundamental property used in operations such as validation, serialization, or entity behavior control.
     */
    val type: Key

    /**
     * Represents the stored value of a shared attribute entity.
     *
     * This property holds the data associated with the attribute, expressed as a string.
     * It is used to define the content or substance of an attribute within a `SharedAttributeEntity`,
     * allowing for flexible storage and retrieval of various types of data in string format.
     * The value can be modified or updated as part of attribute operations.
     */
    var value: String

    /**
     * Represents the timestamp when the shared attribute entity was created.
     *
     * This property is immutable and records the exact moment of creation,
     * providing a reliable reference for tracking the lifecycle of the entity.
     * It is stored as an instance of `Instant` to ensure precision and compatibility
     * with time-based operations.
     */
    val createdAt: Instant

    /**
     * Represents the timestamp of the most recent update to the shared attribute entity.
     *
     * This property indicates when the entity was last modified, ensuring traceability
     * and providing a reliable reference for updates. Stored as an instance of `Instant`,
     * it maintains high precision and supports time-based operations, such as synchronization
     * or version control.
     */
    val updatedAt: Instant

    /**
     * Represents a typed attribute associated with this entity. The attribute holds metadata or
     * additional information about the entity, encapsulated in a key, type, and value structure.
     *
     * The type of the value is determined by the generic parameter of [Attribute], and it ensures
     * compatibility with the associated [dev.gradienttim.gradeway.attribute.AttributeType].
     */
    val attribute: Attribute<*>
}
