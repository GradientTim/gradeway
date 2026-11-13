/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.managers

import arrow.core.Either
import dev.gradienttim.gradeway.utilities.SemVer
import dev.gradienttim.gradeway.utilities.lifecycle.Disableable
import dev.gradienttim.gradeway.utilities.lifecycle.Enableable
import dev.gradienttim.gradeway.utilities.lifecycle.Reloadable
import java.util.concurrent.CompletableFuture

/**
 * Periodically checks for newer Gradeway releases and reports them to the console.
 *
 * Checks are performed asynchronously and are best-effort: a failed check is logged and never
 * affects the rest of Gradeway.
 */
interface UpdateManager : Enableable, Disableable, Reloadable {
    /**
     * The version of the running Gradeway instance.
     */
    val currentVersion: SemVer

    /**
     * The latest release found by the most recent successful check, or `null` if no check has succeeded yet.
     */
    val latestRelease: Release?

    /**
     * Whether the most recent successful check found a release newer than [currentVersion].
     */
    val isUpdateAvailable: Boolean
        get() = latestRelease?.let { it.version > currentVersion } ?: false

    /**
     * Fetches the latest release and updates [latestRelease] on success.
     *
     * @return A future completing with the latest release, or the throwable that caused the check to fail.
     */
    fun check(): CompletableFuture<Either<Throwable, Release>>

    /**
     * A published Gradeway release.
     *
     * @property version The semantic version of the release.
     */
    data class Release(val version: SemVer)
}
