/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.managers

import arrow.core.Either
import arrow.core.raise.either
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.ProjectMeta
import dev.gradienttim.gradeway.platform.Scheduler
import dev.gradienttim.gradeway.update.GitHubReleaseSource
import dev.gradienttim.gradeway.update.ReleaseSource
import dev.gradienttim.gradeway.update.UpdateMessages
import dev.gradienttim.gradeway.utilities.SemVer
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class CommonUpdateManager(
    private val gradeway: CommonGradeway<*>,
    override val currentVersion: SemVer = SemVer.parse(ProjectMeta.VERSION),
    private val releaseSource: ReleaseSource = GitHubReleaseSource(
        repository = REPOSITORY,
        userAgent = "Gradeway/${ProjectMeta.VERSION}",
    ),
) : UpdateManager {
    @Volatile
    override var latestRelease: UpdateManager.Release? = null
        private set

    @Volatile
    private var announcedVersion: SemVer? = null
    private var task: Scheduler.Task? = null

    override fun enable(): Either<Throwable, Unit> = either {
        val config = gradeway.configs.gradewayEntry.config.updateChecker
        if (!config.enabled) {
            return@either
        }

        task = gradeway.scheduler.runTaskTimer(
            interval = config.intervalHours.coerceAtLeast(1),
            intervalUnit = TimeUnit.HOURS,
        ) {
            check()
        }
    }

    override fun disable(): Either<Throwable, Unit> = either {
        task?.cancel()
        task = null
    }

    override fun reload(): Either<Throwable, Unit> = either {
        disable().bind()
        enable().bind()
    }

    override fun check(): CompletableFuture<Either<Throwable, UpdateManager.Release>> =
        releaseSource.fetchLatest().thenApply { result ->
            result
                .onRight { release ->
                    latestRelease = release
                    announceIfNewer(release)
                }
                .onLeft { throwable ->
                    gradeway.logger.warn("Failed to check for Gradeway updates: ${throwable.localizedMessage}")
                }
        }

    private fun announceIfNewer(release: UpdateManager.Release) {
        if (release.version <= currentVersion || release.version == announcedVersion) {
            return
        }
        announcedVersion = release.version
        gradeway.logger.info("A new Gradeway version is available: $currentVersion -> ${release.version}")
        UpdateMessages.DOWNLOAD_PAGES.forEach { (name, url) -> gradeway.logger.info("  $name: $url") }
    }

    companion object {
        const val REPOSITORY = "GradientTim/gradeway"
    }
}
