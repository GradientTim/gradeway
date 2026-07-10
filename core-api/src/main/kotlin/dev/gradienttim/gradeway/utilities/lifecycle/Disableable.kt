/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.utilities.lifecycle

import arrow.core.Either

/**
 * Represents a contract for components that can be deactivated or turned off
 * while remaining loaded, halting their active function without releasing resources.
 *
 * Implementing this interface indicates that the implementing class or entity requires
 * a disabling operation to be performed to become inactive. This is distinct from
 * unloading, as a component may be disabled yet remain loaded and ready to be re-enabled.
 *
 * The `disable` method is expected to return an `Either<Throwable, Unit>` to signify whether
 * the disabling operation was successful or encountered an error. A successful outcome
 * returns `Unit`, while errors or exceptions encountered during the process are encapsulated
 * as a `Throwable` in the `Either` type.
 */
interface Disableable {
    /**
     * Executes the necessary operations to deactivate the component or system.
     *
     * This can involve tasks such as unregistering listeners, stopping scheduled tasks, or
     * otherwise transitioning the component into an inactive state. The disable operation is
     * designed to signal whether it succeeded or failed using an `Either<Throwable, Unit>`
     * return type.
     *
     * @return An `Either<Throwable, Unit>` representing the outcome of the disabling operation.
     *         Returns `Unit` if the operation succeeds, or a `Throwable` if an error occurs
     *         during the process.
     */
    fun disable(): Either<Throwable, Unit>
}
