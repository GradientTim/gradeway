/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.utilities

import arrow.core.Either

/**
 * Represents a contract for components that can be refreshed or
 * re-initialized after their state falls out of sync or becomes stale.
 *
 * Implementing this interface indicates that the implementing class or entity can
 * reset its state, reload configurations, or refresh the necessary resources or data. This process
 * may be needed in scenarios where dynamic state changes require re-synchronization or reinitialization.
 *
 * The `reload` method is designed to return an `Either<Throwable, Unit>` to convey the result of the reload
 * operation. A successful reload returns `Unit`, while failure during the process is captured as a `Throwable`
 * within the `Either`.
 */
interface Reloadable {
    /**
     * Executes the necessary operations to refresh or reinitialize the component or system.
     *
     * This operation is intended to restore the component's state, reload configurations,
     * or refresh resources and data that might have become outdated or inconsistent. It is
     * particularly useful in scenarios where the current state of the component requires
     * alignment or re-synchronization with expected conditions.
     *
     * @return An `Either<Throwable, Unit>` representing the result of the reload operation.
     *         Returns `Unit` if the operation is successful, or a `Throwable` if an error
     *         occurs during the reload process.
     */
    fun reload(): Either<Throwable, Unit>
}
