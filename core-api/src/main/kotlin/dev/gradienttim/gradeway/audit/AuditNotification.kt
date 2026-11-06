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
