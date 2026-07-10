/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.utilities.lifecycle

import arrow.core.Either

/**
 * Represents a contract for components that can be activated or turned on
 * after being loaded, allowing them to begin performing their intended function.
 *
 * Implementing this interface indicates that the implementing class or entity requires
 * an enabling operation to be performed before it becomes active. This is distinct from
 * loading, as a component may be loaded but remain inactive until explicitly enabled.
 *
 * The `enable` method is expected to return an `Either<Throwable, Unit>` to signify whether
 * the enabling operation was successful or encountered an error. A successful outcome
 * returns `Unit`, while errors or exceptions encountered during the process are encapsulated
 * as a `Throwable` in the `Either` type.
 */
interface Enableable {
    /**
     * Executes the necessary operations to activate the component or system.
     *
     * This can involve tasks such as registering listeners, starting scheduled tasks, or
     * otherwise transitioning the component into an active state. The enable operation is
     * designed to signal whether it succeeded or failed using an `Either<Throwable, Unit>`
     * return type.
     *
     * @return An `Either<Throwable, Unit>` representing the outcome of the enabling operation.
     *         Returns `Unit` if the operation succeeds, or a `Throwable` if an error occurs
     *         during the process.
     */
    fun enable(): Either<Throwable, Unit>
}
