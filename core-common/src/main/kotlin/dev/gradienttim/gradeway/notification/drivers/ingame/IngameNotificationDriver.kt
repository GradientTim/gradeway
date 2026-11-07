/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.notification.drivers.ingame

import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.driver.Driver
import dev.gradienttim.gradeway.driver.adapters.NotificationAdapter
import dev.gradienttim.gradeway.notification.NotificationNotifier
import dev.gradienttim.gradeway.platform.Environment

/**
 * Sends notifications as chat messages to online players.
 *
 * Unlike the other built-in notification drivers, this driver needs access to the platform's
 * players, so it is registered by each platform entry point instead of by [CommonGradeway].
 *
 * @param TPlayer The platform's online player type.
 * @param platform Provides access to the platform's online players.
 */
class IngameNotificationDriver<TPlayer>(
    private val platform: IngameNotificationPlatform<TPlayer>,
) : Driver(), NotificationAdapter {
    override fun createNotificationNotifier(environment: Environment): NotificationNotifier =
        IngameNotificationNotifier(gradeway as CommonGradeway<*>, platform)

    companion object {
        const val ID: String = "ingame"
    }
}
