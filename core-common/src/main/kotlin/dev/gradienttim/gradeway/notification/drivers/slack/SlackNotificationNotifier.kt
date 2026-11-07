/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.notification.drivers.slack

import dev.gradienttim.gradeway.audit.AuditNotification
import dev.gradienttim.gradeway.extensions.toPrettyString
import dev.gradienttim.gradeway.notification.*
import dev.gradienttim.gradeway.platform.Logger
import kotlinx.serialization.json.*

class SlackNotificationNotifier(
    logger: Logger,
    private val webhookUrl: String,
) : NotificationNotifier {
    private val client = NotificationHttpClient(logger, "Slack")

    override fun notify(notification: Notification) {
        val title = if (notification is AuditNotification) notification.title else notification.type
        val payload = buildJsonObject {
            put("text", "Gradeway · $title")
            putJsonArray("blocks") {
                addSection("*Gradeway* · $title")
                if (notification is AuditNotification) {
                    addJsonObject {
                        put("type", "section")
                        putJsonArray("fields") {
                            addMarkdown("*Actor*\n${notification.actorLabel}")
                            addMarkdown("*Target*\n${notification.targetLabel}")
                        }
                    }
                    notification.before?.let { addSection("*Before*\n```${it.toPrettyString(MAX_SNAPSHOT_LENGTH)}```") }
                    notification.after?.let { addSection("*After*\n```${it.toPrettyString(MAX_SNAPSHOT_LENGTH)}```") }
                    addJsonObject {
                        put("type", "context")
                        putJsonArray("elements") {
                            addMarkdown(notification.timestamp.toString())
                        }
                    }
                }
            }
        }
        client.postJson(webhookUrl, payload.toString())
    }

    private fun JsonArrayBuilder.addSection(text: String) {
        addJsonObject {
            put("type", "section")
            putJsonObject("text") {
                put("type", "mrkdwn")
                put("text", text)
            }
        }
    }

    private fun JsonArrayBuilder.addMarkdown(text: String) {
        addJsonObject {
            put("type", "mrkdwn")
            put("text", text)
        }
    }

    private companion object {
        // Slack limits section text to 3000 characters; leaves room for the heading and code fences.
        const val MAX_SNAPSHOT_LENGTH = 2900
    }
}
