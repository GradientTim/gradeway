/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.driver.adapters

import dev.gradienttim.gradeway.notification.NotificationNotifier
import dev.gradienttim.gradeway.platform.Environment

interface NotificationAdapter {
    fun createNotificationNotifier(environment: Environment): NotificationNotifier
}
