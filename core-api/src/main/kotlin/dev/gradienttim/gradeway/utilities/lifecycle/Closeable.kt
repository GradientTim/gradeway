/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.utilities.lifecycle

import arrow.core.Either

/**
 * Represents a contract for components that support closing operations.
 *
 * Implementing this interface indicates that the implementing class or entity
 * can be closed, allowing for the release of resources or completion of cleanup tasks.
 * The close operation is distinct from unloading, as it focuses on ensuring any final
 * lifecycle tasks are performed before the component is fully terminated or discarded.
 *
 * The `close` method returns an `Either<Throwable, Unit>` to indicate the success or failure
 * of the close operation. A successful operation returns `Unit`, while any failure or
 * exception encountered during the closing process is captured as a `Throwable` within
 * the `Either` type.
 */
interface Closeable {
    /**
     * Performs the necessary operations to close the component or system.
     *
     * This operation is intended to release resources, complete cleanup tasks,
     * or otherwise transition the component to a closed state. It ensures that any
     * finalization logic is executed before the component is fully terminated or discarded.
     *
     * @return An `Either<Throwable, Unit>` representing the outcome of the close operation.
     *         Returns `Unit` if the operation is successful, or a `Throwable` if an error occurs
     *         during the process.
     */
    fun close(): Either<Throwable, Unit>
}
