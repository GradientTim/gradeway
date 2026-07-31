/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.utilities.lifecycle

import arrow.core.Either

/**
 * Represents a contract for components that can be opened.
 *
 * Implementing this interface indicates that the implementing class or entity
 * supports an operation to transition into an "open" or available state.
 * This may involve actions such as preparing connections, unlocking resources,
 * or making the component accessible for use.
 *
 * The `open` method is expected to return an `Either<Throwable, Unit>` to convey
 * the outcome of the operation. A successful operation returns `Unit`, while any
 * failure or exception during the process is represented as a `Throwable` encapsulated
 * within the `Either`.
 */
interface Openable {
    /**
     * Performs the necessary operations to transition the component or system into an open state.
     *
     * This operation is intended to make the component available for use, which may involve tasks such
     * as establishing connections, unlocking resources, or performing initial setup. The result of the
     * operation indicates whether it succeeded or encountered an error during execution.
     *
     * @return An `Either<Throwable, Unit>` representing the outcome of the open operation.
     *         Returns `Unit` if the operation is successful, or a `Throwable` if an error occurs
     *         during the process.
     */
    fun open(): Either<Throwable, Unit>
}
