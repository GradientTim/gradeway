/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.audit

import kotlinx.serialization.json.JsonObject

/**
 * Represents an auditable entity in the system, which can generate a snapshot
 * of its current state and is associated with a specific target.
 *
 * Implementations of this interface provide a mechanism to capture a
 * representation of their state using the `snapshot` method, typically for
 * logging, auditing, or historical tracking purposes. Each auditable entity
 * is associated with a `Target` that uniquely identifies the entity being audited.
 */
interface Auditable {
    /**
     * Represents the target entity associated with the current object.
     *
     * This property uniquely identifies the entity being referenced or audited,
     * providing contextual information about its type and identifiers. The `Target`
     * class encapsulates details such as the type of the target, a primary identifier,
     * and an optional secondary identifier, offering flexibility for a variety of use cases
     * including grouping, templating, permissions, and more.
     */
    val target: Target

    /**
     * Generates a snapshot of the current state of the object.
     *
     * This method captures and returns a representation of the object's state
     * as a JSON object. The snapshot is typically used for purposes such as
     * logging, auditing, or maintaining historical records.
     *
     * @return A [JsonObject] that represents the current state of the object.
     */
    fun snapshot(): JsonObject
}

/**
 * Captures the state of this entity before and after running [block].
 *
 * @param block The mutation applied to this entity.
 * @return A [Snapshot] holding the state before and after [block] ran.
 */
inline fun <T : Auditable> T.withSnapshot(block: T.() -> Unit): Snapshot {
    val before = snapshot()
    block()
    return Snapshot(target, before, snapshot())
}
