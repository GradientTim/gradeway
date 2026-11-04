/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.notification

import dev.gradienttim.gradeway.audit.AuditNotification
import kotlinx.serialization.json.*

private val PRETTY_JSON = Json { prettyPrint = true }

/** Timestamp fields every snapshot carries; `updatedAt` changes on every update, so both are noise. */
private val IGNORED_CHANGE_FIELDS = setOf("createdAt", "updatedAt")

/**
 * A single field that differs between an audit notification's before and after snapshot,
 * with its values already converted to display text.
 *
 * @property before The previous value, or null if the field was added (e.g. on create).
 * @property after The new value, or null if the field was removed (e.g. on delete).
 */
internal data class AuditFieldChange(
    val field: String,
    val before: String?,
    val after: String?,
)

/**
 * The fields that differ between [AuditNotification.before] and [AuditNotification.after], in
 * snapshot order, without the `createdAt`/`updatedAt` timestamps.
 *
 * On create every field of the new entity is listed with a null [AuditFieldChange.before], on
 * delete every field of the removed entity with a null [AuditFieldChange.after].
 */
internal val AuditNotification.changes: List<AuditFieldChange>
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

private fun JsonElement.toDisplayString(): String = when (this) {
    is JsonNull -> "null"
    is JsonPrimitive -> content
    else -> toString()
}

internal val AuditNotification.title: String
    get() = "$action ${target.type}"

internal val AuditNotification.actorLabel: String
    get() = actor.identifier()?.let { "${actor.type} ($it)" } ?: actor.type

internal val AuditNotification.targetLabel: String
    get() = listOfNotNull(target.id, target.secondaryId).joinToString(" / ")

/** One-line description such as `player (<uuid>) update role (<uuid>)`. */
internal val AuditNotification.summary: String
    get() = "$actorLabel $action ${target.type} ($targetLabel)"

internal fun JsonObject.toPrettyString(maxLength: Int): String {
    val text = PRETTY_JSON.encodeToString(JsonObject.serializer(), this)
    return if (text.length <= maxLength) text else text.take(maxLength - 1) + "…"
}
