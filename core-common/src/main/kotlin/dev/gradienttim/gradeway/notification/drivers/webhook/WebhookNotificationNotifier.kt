/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.notification.drivers.webhook

import dev.gradienttim.gradeway.notification.Notification
import dev.gradienttim.gradeway.notification.NotificationHttpClient
import dev.gradienttim.gradeway.notification.NotificationNotifier
import dev.gradienttim.gradeway.platform.Logger
import dev.gradienttim.gradeway.utilities.Signer

class WebhookNotificationNotifier(
    logger: Logger,
    private val url: String,
    secret: String?,
) : NotificationNotifier {
    private val client = NotificationHttpClient(logger, "webhook")
    private val signer = secret?.let(::Signer)

    override fun notify(notification: Notification) {
        val body = notification.toJson().toString()
        val headers = buildMap {
            put("X-Gradeway-Event", notification.type)
            signer?.let {
                put("X-Gradeway-Signature", "sha256=${it.signature(body.toByteArray()).toHexString()}")
            }
        }

        client.postJson(url, body, headers)
    }
}
