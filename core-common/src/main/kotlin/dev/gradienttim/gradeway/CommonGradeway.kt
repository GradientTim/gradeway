/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.either
import dev.gradienttim.gradeway.audit.Actor
import dev.gradienttim.gradeway.configs.PlatformConfig
import dev.gradienttim.gradeway.constants.ScheduleConstants
import dev.gradienttim.gradeway.driver.meta.DriverType
import dev.gradienttim.gradeway.event.handleRemoteEvent
import dev.gradienttim.gradeway.events.lifecycle.GradewayDisabledEvent
import dev.gradienttim.gradeway.events.lifecycle.GradewayEnabledEvent
import dev.gradienttim.gradeway.events.lifecycle.GradewayLoadedEvent
import dev.gradienttim.gradeway.events.lifecycle.GradewayReloadedEvent
import dev.gradienttim.gradeway.managers.*
import dev.gradienttim.gradeway.notification.drivers.console.ConsoleNotificationDriver
import dev.gradienttim.gradeway.notification.drivers.discord.DiscordNotificationDriver
import dev.gradienttim.gradeway.notification.drivers.slack.SlackNotificationDriver
import dev.gradienttim.gradeway.notification.drivers.webhook.WebhookNotificationDriver
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
    override val stores: Stores,
    override val scheduler: Scheduler,
    override val directory: Path,
    override val defaultPlatformConfig: TPlatformConfig,
    override val platformConfigSerializer: KSerializer<TPlatformConfig>,
    private val knownPermissionSource: KnownPermissionSource,
) : GradewayLifecycle<TPlatformConfig>, KoinComponent {
    override val now: () -> Instant = { Instant.now() }
    override var state: GradewayState = GradewayState.UNLOADED
    override val events: EventManager = CommonEventManager(logger)
    override val caches: Caches by inject()
    override val environment by lazy { CommonEnvironment(this) }

    override val permissions: PermissionService by inject()
    override val attributes: AttributeService by inject()
    override val players: PlayerService by inject()
    override val groups: GroupService by inject()
    override val tracks: TrackService by inject()
    override val roles: RoleService by inject()
    override val logs: LogService by inject()

    override val knownPermissions: KnownPermissionsManager by inject()
    override val notifications: NotificationManager by inject()
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
        messaging.subscribe { payload -> handleRemoteEvent(payload) }

        drivers.registerDriver("console", DriverType.NOTIFICATION, ConsoleNotificationDriver())
        drivers.registerDriver("webhook", DriverType.NOTIFICATION, WebhookNotificationDriver())
        drivers.registerDriver("discord", DriverType.NOTIFICATION, DiscordNotificationDriver())
        drivers.registerDriver("slack", DriverType.NOTIFICATION, SlackNotificationDriver())

        state = GradewayState.LOADED
        events.fire(GradewayLoadedEvent())
    }.onLeft { throwable ->
        logger.panic(throwable)
        state = GradewayState.UNLOADED
    }

    override fun unload(): Either<Throwable, Unit> = either {
        if (!state.allowUnload) raise(GradewayAlreadyUnloadedThrowable())
        state = GradewayState.PROCESSING

        caches.invalidateAll()

        knownPermissions.unload().bind()
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
        notifications.reload().bind()

        events.fire(GradewayReloadedEvent())
    }.onLeft { throwable ->
        logger.panic(throwable)
    }

    override fun enable(): Either<Throwable, Unit> = either {
        checkIsLoaded()

        databases.enable().bind()
        messaging.enable().bind()
        notifications.enable().bind()

        caches.suggestions.initialize()
        knownPermissions.seed(knownPermissionSource.collect())

        expiredRoleSweepTask = scheduler.runTaskTimer(
            interval = ScheduleConstants.EXPIRED_ROLE_SWEEP_INTERVAL_SECONDS,
            intervalUnit = TimeUnit.SECONDS
        ) {
            players.removeExpiredRoles(Actor.System).onLeft {
                logger.warn("Failed to sweep expired player roles: $it")
            }
        }

        events.fire(GradewayEnabledEvent())
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
        notifications.disable().bind()

        events.fire(GradewayDisabledEvent())
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
                single<LogService> { CommonLogService(gradeway) }
            },

            module {
                single<KnownPermissionsManager> { CommonKnownPermissionManager() }
                single<NotificationManager> { CommonNotificationManager(gradeway) }
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
