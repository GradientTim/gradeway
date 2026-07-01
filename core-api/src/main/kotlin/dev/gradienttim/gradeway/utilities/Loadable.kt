/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.utilities

import arrow.core.Either

/**
 * Represents a contract for components that can be initialized or prepared for use.
 *
 * Implementing this interface indicates that the implementing class or entity requires
 * a loading operation to be performed before it can be used. This operation may involve
 * tasks such as resource initialization, data loading, or establishing connections.
 *
 * The `load` method is expected to return an `Either<Throwable, Unit>` to signify whether
 * the loading operation was successful or encountered an error. A successful outcome
 * returns `Unit`, while errors or exceptions encountered during the process are encapsulated
 * as a `Throwable` in the `Either` type.
 */
interface Loadable {
    /**
     * Executes the necessary operations to prepare the component or system for use.
     *
     * This can involve tasks such as initializing resources, loading data, or establishing
     * required connections. The load operation is designed to signal whether it succeeded
     * or failed using an `Either<Throwable, Unit>` return type.
     *
     * @return An `Either<Throwable, Unit>` representing the outcome of the loading operation.
     *         Returns `Unit` if the operation succeeds, or a `Throwable` if an error occurs
     *         during the process.
     */
    fun load(): Either<Throwable, Unit>
}
