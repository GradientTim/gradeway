/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.notification.drivers.console

import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.driver.Driver
import dev.gradienttim.gradeway.driver.adapters.NotificationAdapter
import dev.gradienttim.gradeway.notification.NotificationNotifier
import dev.gradienttim.gradeway.platform.Environment

class ConsoleNotificationDriver : Driver(), NotificationAdapter {
    override fun createNotificationNotifier(environment: Environment): NotificationNotifier =
        ConsoleNotificationNotifier((gradeway as CommonGradeway<*>).logger)
}
