/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.notification.drivers.discord

import dev.gradienttim.gradeway.audit.AuditNotification
import dev.gradienttim.gradeway.constants.ActionConstants
import dev.gradienttim.gradeway.notification.*
import dev.gradienttim.gradeway.platform.Logger
import kotlinx.serialization.json.*

class DiscordNotificationNotifier(
    logger: Logger,
    private val webhookUrl: String,
) : NotificationNotifier {
    private val client = NotificationHttpClient(logger, "Discord")

    override fun notify(notification: Notification) {
        val payload = buildJsonObject {
            putJsonArray("embeds") {
                addJsonObject {
                    put("color", notification.embedColor())
                    if (notification is AuditNotification) {
                        put("title", "Gradeway")
                        put("timestamp", notification.timestamp.toString())
                        putJsonArray("fields") {
                            addField("Actor", notification.actorLabel)
                            addField("Target", "${notification.target.type} (${notification.targetLabel})")
                            addField("Action", notification.action)
                            notification.before?.let { addField("Before", it.toCodeBlock()) }
                            notification.after?.let { addField("After", it.toCodeBlock()) }
                        }
                    } else {
                        put("title", "Gradeway · ${notification.type}")
                    }
                }
            }
        }
        client.postJson(webhookUrl, payload.toString())
    }

    private fun JsonArrayBuilder.addField(name: String, value: String, inline: Boolean = false) {
        addJsonObject {
            put("name", name)
            put("value", value)
            put("inline", inline)
        }
    }

    private fun JsonObject.toCodeBlock(): String =
        "```json\n${toPrettyString(MAX_FIELD_LENGTH - CODE_BLOCK_OVERHEAD)}\n```"

    private fun Notification.embedColor(): Int = when ((this as? AuditNotification)?.action) {
        ActionConstants.CREATE -> CREATE_COLOR
        ActionConstants.UPDATE -> UPDATE_COLOR
        ActionConstants.DELETE -> DELETE_COLOR
        else -> DEFAULT_COLOR
    }

    private companion object {
        const val CREATE_COLOR = 0x57F287
        const val UPDATE_COLOR = 0xFEE75C
        const val DELETE_COLOR = 0xED4245
        const val DEFAULT_COLOR = 0xED751F
        const val MAX_FIELD_LENGTH = 1024
        const val CODE_BLOCK_OVERHEAD = 12
    }
}
