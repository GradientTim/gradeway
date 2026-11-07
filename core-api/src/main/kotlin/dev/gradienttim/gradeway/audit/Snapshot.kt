/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.audit

import kotlinx.serialization.json.JsonObject

/**
 * Represents the state of an audited [Target] before and after an operation.
 *
 * A creation has no [before] state, a deletion has no [after] state.
 */
data class Snapshot(
    val target: Target,
    val before: JsonObject?,
    val after: JsonObject?,
) {
    companion object {
        /**
         * Creates a snapshot for a newly created [entity], with no previous state.
         */
        fun created(entity: Auditable) = Snapshot(entity.target, null, entity.snapshot())

        /**
         * Creates a snapshot for an [entity] about to be deleted, with no following state.
         */
        fun deleted(entity: Auditable) = Snapshot(entity.target, entity.snapshot(), null)
    }
}
