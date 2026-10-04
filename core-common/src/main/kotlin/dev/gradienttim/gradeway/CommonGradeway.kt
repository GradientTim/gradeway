/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.either
import dev.gradienttim.gradeway.configs.PlatformConfig
import dev.gradienttim.gradeway.constants.ScheduleConstants
import dev.gradienttim.gradeway.managers.*
import dev.gradienttim.gradeway.platform.*
import dev.gradienttim.gradeway.services.*
import dev.gradienttim.gradeway.throwables.GradewayAlreadyLoadedThrowable
import dev.gradienttim.gradeway.throwables.GradewayAlreadyUnloadedThrowable
import dev.gradienttim.gradeway.throwables.GradewayNotLoadedThrowable
import kotlinx.serialization.KSerializer
import net.kyori.adventure.text.minimessage.MiniMessage
import org.jetbrains.exposed.v1.jdbc.Database
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.Module
import org.koin.dsl.module
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.concurrent.TimeUnit

class CommonGradeway<TPlatformConfig : PlatformConfig>(
    override val logger: Logger,
    override val scheduler: Scheduler,
    override val directory: Path,
    override val defaultPlatformConfig: TPlatformConfig,
    override val platformConfigSerializer: KSerializer<TPlatformConfig>,
) : GradewayLifecycle<TPlatformConfig>, KoinComponent {
    override val now: () -> Instant = { Instant.now() }
    override var state: GradewayState = GradewayState.UNLOADED
    override val caches: Caches by inject()
    override val environment by lazy { CommonEnvironment(this) }

    override val permissions: PermissionService by inject()
    override val attributes: AttributeService by inject()
    override val players: PlayerService by inject()
    override val groups: GroupService by inject()
    override val tracks: TrackService by inject()
    override val roles: RoleService by inject()

    override val confirmations: ConfirmationManager by inject()
    override val migrations: MigrationManager by inject()
    override val messaging: MessagingManager by inject()
    override val databases: DatabaseManager by inject()
    override val languages: LanguageManager by inject()
    override val drivers: DriverManager by inject()
    override val configs: ConfigManager<TPlatformConfig> by inject()
    override val backups: BackupManager by inject()

    internal lateinit var miniMessage: MiniMessage
    internal lateinit var database: Database

    private var expiredRoleSweepTask: Scheduler.Task? = null

    override fun load(): Either<Throwable, Unit> = either {
        if (!state.allowLoad) raise(GradewayAlreadyLoadedThrowable())
        state = GradewayState.PROCESSING

        if (!Files.exists(directory)) {
            Files.createDirectory(directory)
        }

        startKoin {
            modules(koinModules(this@CommonGradeway))
        }

        configs.load().bind()
        drivers.load().bind()
        languages.load().bind()
        messaging.load().bind()

        state = GradewayState.LOADED
    }.onLeft { throwable ->
        logger.panic(throwable)
        state = GradewayState.UNLOADED
    }

    override fun unload(): Either<Throwable, Unit> = either {
        if (!state.allowUnload) raise(GradewayAlreadyUnloadedThrowable())
        state = GradewayState.PROCESSING

        caches.invalidateAll()

        messaging.unload().bind()
        languages.unload().bind()
        drivers.unload().bind()

        stopKoin()

        state = GradewayState.UNLOADED
    }.onLeft { throwable ->
        logger.panic(throwable)
        state = GradewayState.LOADED
    }

    override fun reload(): Either<Throwable, Unit> = either {
        checkIsLoaded()

        configs.load().bind()
        messaging.reload().bind()
        languages.reload().bind()
    }.onLeft { throwable ->
        logger.panic(throwable)
    }

    override fun enable(): Either<Throwable, Unit> = either {
        checkIsLoaded()

        databases.enable().bind()
        messaging.enable().bind()

        caches.suggestions.initialize()

        expiredRoleSweepTask = scheduler.runTaskTimer(
            interval = ScheduleConstants.EXPIRED_ROLE_SWEEP_INTERVAL_SECONDS,
            intervalUnit = TimeUnit.SECONDS
        ) {
            players.removeExpiredRoles().onLeft {
                logger.warn("Failed to sweep expired player roles: $it")
            }
        }
    }.onLeft { throwable ->
        logger.panic(throwable)
    }

    override fun disable(): Either<Throwable, Unit> = either {
        checkIsLoaded()

        expiredRoleSweepTask?.cancel()
        expiredRoleSweepTask = null

        databases.disable().bind()
        messaging.disable().bind()
        confirmations.disable().bind()
    }.onLeft { throwable ->
        logger.panic(throwable)
    }

    private fun Raise<Throwable>.checkIsLoaded() {
        if (state != GradewayState.LOADED) {
            raise(GradewayNotLoadedThrowable())
        }
    }

    private fun koinModules(gradeway: CommonGradeway<TPlatformConfig>): List<Module> {
        return listOf(
            module {
                single<PermissionService> { CommonPermissionService(gradeway) }
                single<AttributeService> { CommonAttributeService(gradeway) }
                single<PlayerService> { CommonPlayerService(gradeway) }
                single<GroupService> { CommonGroupService(gradeway) }
                single<RoleService> { CommonRoleService(gradeway) }
                single<TrackService> { CommonTrackService(gradeway) }
            },

            module {
                single<ConfirmationManager> { CommonConfirmationManager(gradeway) }
                // createdAtStart: the migrate command looks strategies up in MigrationStrategyRegistry directly,
                // before ever touching gradeway.migrations, so the registrations CommonMigrationManager's init
                // block performs must already have happened - a lazily created single would run them too late.
                single<MigrationManager>(createdAtStart = true) { CommonMigrationManager(gradeway) }
                single<MessagingManager> { CommonMessagingManager(gradeway) }
                single<DatabaseManager> { CommonDatabaseManager(gradeway) }
                single<LanguageManager> { CommonLanguageManager(gradeway) }
                single<DriverManager> { CommonDriverManager(gradeway) }
                single<ConfigManager<TPlatformConfig>> { CommonConfigManager(gradeway) }
                single<BackupManager> { CommonBackupManager(gradeway) }
            },

            module {
                single<Caches> { CommonCaches(gradeway) }
                single<Gradeway<TPlatformConfig>> { gradeway }
            }
        )
    }
}
