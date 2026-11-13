/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.update

import arrow.core.Either
import dev.gradienttim.gradeway.managers.UpdateManager
import java.util.concurrent.CompletableFuture

/**
 * Source of published Gradeway releases used by update checks.
 *
 * Implementations retrieve the latest available release asynchronously and report the result as
 * either a failure or a resolved [UpdateManager.Release].
 */
fun interface ReleaseSource {
    /**
     * Retrieves the latest available Gradeway release asynchronously.
     *
     * @return A future completing with the latest release, or the throwable that caused retrieval to fail.
     */
    fun fetchLatest(): CompletableFuture<Either<Throwable, UpdateManager.Release>>
}
