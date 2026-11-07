/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.audit

import dev.gradienttim.gradeway.notification.Notification
import kotlinx.serialization.json.*
import java.time.Instant

/**
 * A notification sent for every recorded audit log entry.
 *
 * @property actor The actor that performed the operation.
 * @property action The action that was performed (e.g. "create", "update", "delete").
 * @property target The entity the operation was performed on.
 * @property before The state of the target before the operation, or null for a creation.
 * @property after The state of the target after the operation, or null for a deletion.
 * @property timestamp The point in time the operation was recorded.
 */
data class AuditNotification(
    val actor: Actor,
    val action: String,
    val target: Target,
    val before: JsonObject?,
    val after: JsonObject?,
    val timestamp: Instant,
) : Notification {
    override val type: String = "audit"

    val title: String
        get() = "$action ${target.type}"

    val actorLabel: String
        get() = actor.identifier()?.let { "${actor.type} ($it)" } ?: actor.type

    val targetLabel: String
        get() = listOfNotNull(target.id, target.secondaryId).joinToString(" / ")

    /** One-line description such as `player (<uuid>) update role (<uuid>)`. */
    val summary: String
        get() = "$actorLabel $action ${target.type} ($targetLabel)"

    /**
     * The fields that differ between [before] and [after], in snapshot order, without the
     * `createdAt`/`updatedAt` timestamps.
     *
     * On create every field of the new entity is listed with a null [AuditFieldChange.before], on
     * delete every field of the removed entity with a null [AuditFieldChange.after].
     */
    val changes: List<AuditFieldChange>
        get() {
            val beforeFields = before.orEmpty()
            val afterFields = after.orEmpty()

            return (beforeFields.keys + afterFields.keys)
                .filter { field -> field !in IGNORED_CHANGE_FIELDS }
                .mapNotNull { field ->
                    val beforeValue = beforeFields[field]
                    val afterValue = afterFields[field]
                    if (beforeValue == afterValue) {
                        null
                    } else {
                        AuditFieldChange(field, beforeValue?.toDisplayString(), afterValue?.toDisplayString())
                    }
                }
        }

    override fun toJson(): JsonObject = buildJsonObject {
        put("type", type)
        put("action", action)
        put("timestamp", timestamp.toString())
        putJsonObject("actor") {
            put("type", actor.type)
            put("identifier", actor.identifier())
        }
        putJsonObject("target") {
            put("type", target.type)
            put("id", target.id.toString())
            put("secondaryId", target.secondaryId?.toString())
        }
        put("before", before ?: JsonNull)
        put("after", after ?: JsonNull)
    }
}

/** Timestamp fields every snapshot carries; `updatedAt` changes on every update, so both are noise. */
private val IGNORED_CHANGE_FIELDS = setOf("createdAt", "updatedAt")

private fun JsonElement.toDisplayString(): String = when (this) {
    is JsonNull -> "null"
    is JsonPrimitive -> content
    else -> toString()
}
