/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.notification.drivers.webhook

import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.driver.Driver
import dev.gradienttim.gradeway.driver.adapters.NotificationAdapter
import dev.gradienttim.gradeway.notification.NotificationNotifier
import dev.gradienttim.gradeway.platform.Environment

class WebhookNotificationDriver : Driver(), NotificationAdapter {
    override fun createNotificationNotifier(environment: Environment): NotificationNotifier {
        val url = environment.stringRequired("GRADEWAY_NOTIFICATION_WEBHOOK_URL")
        val secret = environment.string("GRADEWAY_NOTIFICATION_WEBHOOK_SECRET")?.takeIf { it.isNotBlank() }
        return WebhookNotificationNotifier((gradeway as CommonGradeway<*>).logger, url, secret)
    }
}
