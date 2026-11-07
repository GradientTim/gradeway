/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.platform

import arrow.core.Either
import arrow.core.raise.either
import dev.gradienttim.gradeway.utilities.Store
import dev.gradienttim.gradeway.utilities.Store.ClearError
import java.util.*

/**
 * Provides access to various key-value stores for managing application data.
 *
 * This interface defines a centralized point for accessing different types of
 * stores, each identified by a specific key-value relationship.
 */
interface Stores {
    /**
     * A key-value store for managing notification states, where each key is a unique identifier (UUID)
     * and the value is a boolean representing the notification's state.
     *
     * The `notification` store allows storage, retrieval, removal, and toggling of notification states.
     * A `true` value represents an active or enabled notification, while `false` represents an inactive
     * or disabled notification. This store is particularly useful in scenarios where individual states
     * for notifications need to be tracked and modified dynamically.
     */
    val notification: Store<UUID, Boolean>

    /**
     * Clears all entries from the notification store.
     *
     * This operation removes all stored notification states. If clearing the store fails,
     * the failure is returned as a `ClearError`.
     *
     * @return An `Either` containing `Unit` when all notification entries are cleared successfully,
     * or a `ClearError` if the operation fails.
     */
    fun clearAll(): Either<ClearError, Unit> = either {
        notification.clear().bind()
    }
}
