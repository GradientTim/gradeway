/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.utilities

import arrow.core.Either

/**
 * Represents a contract for components that support unloading operations.
 *
 * Implementing this interface indicates that the implementing class or entity can release
 * its resources, perform cleanup tasks, or shut down gracefully. The unloading operation
 * is particularly useful in scenarios where the component is no longer needed, or the application
 * needs to free up resources.
 *
 * The `unload` method is expected to return an `Either<Throwable, Unit>` that represents
 * the result of the unloading process. A successful unloading returns `Unit`, whereas any errors
 * or exceptions encountered during the process are encapsulated as a `Throwable` in the `Either` type.
 */
interface Unloadable {
    /**
     * Executes the operations required to unload the component or system.
     *
     * This operation is intended to release resources, perform cleanup tasks, or shut down
     * the component gracefully. It is typically used when the component is no longer needed
     * or the application is freeing up occupied resources. The method ensures proper state
     * transitions and error handling during the unloading process.
     *
     * @return An `Either<Throwable, Unit>` representing the result of the unload operation.
     *         Returns `Unit` if the operation is successful, or a `Throwable` if an error
     *         occurs during the unloading process.
     */
    fun unload(): Either<Throwable, Unit>
}
