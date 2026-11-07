/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.audit

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.either
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.constants.ActionConstants
import dev.gradienttim.gradeway.event.PostEvent
import dev.gradienttim.gradeway.event.dispatchPostEvents
import kotlinx.serialization.json.JsonObject

/**
 * Collects the audit entries of a single service operation while it runs.
 *
 * Entries are only buffered here; [audited] writes them once the operation has succeeded, so a
 * failed or rolled-back operation never shows up in the audit log or sends a notification.
 */
internal class AuditRecorder {
    private val entries = mutableListOf<Pair<String, Snapshot>>()
    private val postEvents = mutableListOf<PostEvent>()

    /**
     * Records the creation of [entity]. Call it after the entity has been created.
     */
    fun created(entity: Auditable) {
        entries += ActionConstants.CREATE to Snapshot.created(entity)
    }

    /**
     * Records the deletion of [entity]. Call it before the entity is deleted.
     */
    fun deleted(entity: Auditable) {
        entries += ActionConstants.DELETE to Snapshot.deleted(entity)
    }

    /**
     * Records an update of [entity], comparing its current state against [before].
     * Call it after the update; nothing is recorded if the state did not change.
     */
    fun updated(entity: Auditable, before: JsonObject) {
        val after = entity.snapshot()
        if (before != after) {
            entries += ActionConstants.UPDATE to Snapshot(entity.target, before, after)
        }
    }

    /**
     * Queues [event] to be fired once the operation succeeded. Discarded when the operation raises.
     */
    fun emit(event: PostEvent) {
        postEvents += event
    }

    fun dispatch(gradeway: CommonGradeway<*>) {
        gradeway.dispatchPostEvents(postEvents.toList())
    }

    fun write(gradeway: CommonGradeway<*>, actor: Actor) {
        if (entries.isEmpty() || !gradeway.configs.gradewayEntry.config.auditLog.enabled) {
            return
        }
        entries.forEach { (action, snapshot) ->
            gradeway.logs.createAuditLog(actor, action, snapshot).onLeft { error ->
                gradeway.logger.warn(
                    "Failed to write audit log '$action' for ${snapshot.target.type} " +
                        "'${snapshot.target.id}': $error",
                )
            }
        }
    }
}

/**
 * A drop-in replacement for Arrow's `either { }` that also writes the audit entries the operation
 * recorded, once it succeeded.
 *
 * A failing audit write is only logged, it never changes the result of the operation.
 *
 * @param actor The actor performing the operation.
 * @param block The operation, recording its changes on the provided [AuditRecorder].
 * @return The result of [block], unchanged.
 */
internal inline fun <E, T> CommonGradeway<*>.audited(
    actor: Actor,
    block: Raise<E>.(audit: AuditRecorder) -> T,
): Either<E, T> {
    val recorder = AuditRecorder()
    return either { block(recorder) }.onRight {
        recorder.write(this, actor)
        recorder.dispatch(this)
    }
}
