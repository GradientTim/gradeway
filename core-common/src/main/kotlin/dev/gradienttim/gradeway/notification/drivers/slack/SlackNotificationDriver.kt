/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.notification.drivers.slack

import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.driver.Driver
import dev.gradienttim.gradeway.driver.adapters.NotificationAdapter
import dev.gradienttim.gradeway.notification.NotificationNotifier
import dev.gradienttim.gradeway.platform.Environment

class SlackNotificationDriver : Driver(), NotificationAdapter {
    override fun createNotificationNotifier(environment: Environment): NotificationNotifier {
        val webhookUrl = environment.stringRequired("GRADEWAY_NOTIFICATION_SLACK_WEBHOOK_URL")
        return SlackNotificationNotifier((gradeway as CommonGradeway<*>).logger, webhookUrl)
    }
}
