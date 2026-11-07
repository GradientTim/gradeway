/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.managers

import arrow.core.Either
import arrow.core.raise.either
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.audit.AuditNotification
import dev.gradienttim.gradeway.driver.adapters.NotificationAdapter
import dev.gradienttim.gradeway.driver.meta.DriverType
import dev.gradienttim.gradeway.notification.Notification
import dev.gradienttim.gradeway.notification.NotificationNotifier
import dev.gradienttim.gradeway.utilities.Store
import dev.gradienttim.gradeway.utilities.toggle
import java.util.*

class CommonNotificationManager(val gradeway: CommonGradeway<*>) : NotificationManager {
    @Volatile
    private var notifiers: List<NamedNotifier> = emptyList()

    override fun enable(): Either<Throwable, Unit> = either {
        val config = gradeway.configs.driversEntry.config.notification
        if (!config.enabled) {
            return@either
        }

        // A notifier that cannot be created is logged and skipped instead of raised: notifications
        // are best-effort and must never keep Gradeway itself from enabling.
        notifiers = config.drivers.distinct().mapNotNull { driverId -> createNotifier(driverId) }
        gradeway.logger.info("Enabled ${notifiers.size} notification driver(s).")
    }

    override fun disable(): Either<Throwable, Unit> = either {
        notifiers = emptyList()
    }

    override fun reload(): Either<Throwable, Unit> = either {
        disable().bind()
        enable().bind()
    }

    override fun notify(notification: Notification) {
        val currentNotifiers = notifiers
        if (currentNotifiers.isEmpty() || !isAllowed(notification)) {
            return
        }

        currentNotifiers.forEach { (driverId, notifier) ->
            runCatching { notifier.notify(notification) }.onFailure { throwable ->
                gradeway.logger.error(
                    "Failed to send notification '${notification.type}' via driver '$driverId': " +
                            throwable.localizedMessage,
                )
            }
        }
    }

    override fun isReceiving(uniqueId: UUID): Boolean =
        gradeway.stores.notification.get(uniqueId) ?: false

    override fun setReceiving(uniqueId: UUID, state: Boolean): Either<Store.SetError, Boolean> = either {
        if (!gradeway.stores.notification.set(uniqueId, state).bind()) {
            raise(
                Store.SetError.Unexpected(
                    IllegalStateException("The notification state for '$uniqueId' could not be stored."),
                ),
            )
        }
        state
    }

    override fun toggleReceiving(uniqueId: UUID): Either<Store.SetError, Boolean> =
        gradeway.stores.notification.toggle(uniqueId)

    private fun createNotifier(driverId: String): NamedNotifier? {
        if (driverId.isBlank()) {
            gradeway.logger.warn("Skipping notification driver with a blank id.")
            return null
        }

        val driver = gradeway.drivers.findDriver(driverId, DriverType.NOTIFICATION)
        if (driver == null) {
            gradeway.logger.error("Unable to enable notification driver '$driverId': Driver not found.")
            return null
        }

        if (driver !is NotificationAdapter) {
            gradeway.logger.error(
                "Unable to enable notification driver '$driverId': Driver does not implement NotificationAdapter.",
            )
            return null
        }

        return runCatching { NamedNotifier(driverId, driver.createNotificationNotifier(gradeway.environment)) }
            .onFailure { throwable ->
                gradeway.logger.error(
                    "Unable to enable notification driver '$driverId': ${throwable.localizedMessage}",
                )
            }
            .getOrNull()
    }

    private fun isAllowed(notification: Notification): Boolean {
        return when (notification) {
            is AuditNotification -> {
                val config = gradeway.configs.driversEntry.config.notification
                val targetAllowed = config.targets.isEmpty() || notification.target.type in config.targets
                val actionAllowed = config.actions.isEmpty() || notification.action in config.actions
                targetAllowed && actionAllowed
            }

            else -> true
        }
    }

    private data class NamedNotifier(
        val driverId: String,
        val notifier: NotificationNotifier,
    )
}
