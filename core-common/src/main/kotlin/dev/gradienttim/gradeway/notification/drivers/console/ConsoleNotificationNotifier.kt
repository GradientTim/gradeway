/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.notification.drivers.console

import dev.gradienttim.gradeway.audit.AuditNotification
import dev.gradienttim.gradeway.notification.Notification
import dev.gradienttim.gradeway.notification.NotificationNotifier
import dev.gradienttim.gradeway.platform.Logger

/**
 * Writes every notification to the plugin logger, without contacting any external service.
 * Useful for checking the `targets`/`actions` notification filters.
 */
class ConsoleNotificationNotifier(
    private val logger: Logger,
) : NotificationNotifier {
    override fun notify(notification: Notification) {
        logger.info(format(notification))
    }

    private fun format(notification: Notification): String = when (notification) {
        is AuditNotification -> "[notification] ${notification.summary}"
        else -> "[notification] ${notification.type}"
    }
}
