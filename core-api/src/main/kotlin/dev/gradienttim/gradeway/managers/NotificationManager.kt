/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.managers

import arrow.core.Either
import dev.gradienttim.gradeway.notification.Notification
import dev.gradienttim.gradeway.utilities.Store
import dev.gradienttim.gradeway.utilities.lifecycle.Disableable
import dev.gradienttim.gradeway.utilities.lifecycle.Enableable
import dev.gradienttim.gradeway.utilities.lifecycle.Reloadable
import java.util.*

/**
 * Manages the configured notification drivers and dispatches notifications to them.
 *
 * Enabling the manager creates one notifier per driver listed in the notification section of the
 * drivers' config. Notifications are delivered to every notifier, unless they are excluded by the
 * configured target or action filters.
 */
interface NotificationManager : Enableable, Disableable, Reloadable {
    /**
     * Dispatches the provided notification to every configured notifier.
     *
     * Delivery is best-effort: a failing notifier is logged and never affects the other notifiers
     * or the caller.
     *
     * @param notification The notification instance to be dispatched.
     */
    fun notify(notification: Notification)

    /**
     * Checks whether a notifier associated with the given unique identifier is currently receiving notifications.
     *
     * @param uniqueId The unique identifier of the notifier to check.
     * @return True if the notifier is receiving notifications, false otherwise.
     */
    fun isReceiving(uniqueId: UUID): Boolean

    /**
     * Sets the receiving state for a notifier associated with the given unique identifier.
     *
     * @param uniqueId The unique identifier of the notifier whose receiving state is to be updated.
     * @param state The desired receiving state; true to enable receiving notifications, false to disable.
     * @return An Either containing a SetError if the operation fails, or the resulting receiving state
     *         (true if notifications are now enabled, false if they are now disabled).
     */
    fun setReceiving(uniqueId: UUID, state: Boolean): Either<Store.SetError, Boolean>

    /**
     * Toggles the receiving state for a notifier associated with the given unique identifier.
     * If the notifier is currently set to receive notifications, it will be disabled; if it is
     * currently disabled, it will be enabled.
     *
     * @param uniqueId The unique identifier of the notifier whose receiving state is to be toggled.
     * @return An Either containing a SetError if the operation fails, or the resulting receiving state
     *         after toggling (true if notifications are now enabled, false if they are now disabled).
     */
    fun toggleReceiving(uniqueId: UUID): Either<Store.SetError, Boolean>
}
