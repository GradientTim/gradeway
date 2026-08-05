/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.either
import dev.gradienttim.gradeway.managers.*
import dev.gradienttim.gradeway.platform.*
import dev.gradienttim.gradeway.services.*
import dev.gradienttim.gradeway.throwables.GradewayAlreadyLoadedThrowable
import dev.gradienttim.gradeway.throwables.GradewayAlreadyUnloadedThrowable
import dev.gradienttim.gradeway.throwables.GradewayNotLoadedThrowable
import kotlinx.coroutines.*
import kotlinx.serialization.KSerializer
import net.kyori.adventure.text.minimessage.MiniMessage
import org.jetbrains.exposed.v1.jdbc.Database
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.io.File
import java.time.Instant
import kotlin.time.Duration.Companion.milliseconds

class CommonGradeway<TPlatformConfig>(
    override val logger: Logger,
    override val directory: File,
    override val defaultPlatformConfig: TPlatformConfig,
    val platformConfigSerializer: KSerializer<TPlatformConfig>,
) : GradewayLifecycle<TPlatformConfig>, KoinComponent {
    override val now: () -> Instant = { Instant.now() }
    override val caches: Caches by inject()
    override var backgroundScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override val permissions: PermissionService by inject()
    override val attributes: AttributeService by inject()
    override val players: PlayerService by inject()
    override val groups: GroupService by inject()
    override val roles: RoleService by inject()

    override val confirmations: ConfirmationManager by inject()
    override val migrations: MigrationManager by inject()
    override val messaging: MessagingManager by inject()
    override val databases: DatabaseManager by inject()
    override val languages: LanguageManager by inject()
    override val drivers: DriverManager by inject()
    override val configs: ConfigManager<TPlatformConfig> by inject()
    override val backups: BackupManager by inject()

    override val databaseEnvironment by lazy { CommonEnvironment(this, Environment.Type.DATABASE) }
    override val messagingEnvironment by lazy { CommonEnvironment(this, Environment.Type.MESSAGING) }

    override var state: GradewayState = GradewayState.UNLOADED

    internal lateinit var miniMessage: MiniMessage
    internal lateinit var database: Database

    private var expiredRoleSweepJob: Job? = null

    override fun load(): Either<Throwable, Unit> = either {
        if (!state.allowLoad) raise(GradewayAlreadyLoadedThrowable())
        state = GradewayState.PROCESSING

        backgroundScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        val serviceModule = module {
            single<PermissionService> { CommonPermissionService(this@CommonGradeway) }
            single<AttributeService> { CommonAttributeService(this@CommonGradeway) }
            single<PlayerService> { CommonPlayerService(this@CommonGradeway) }
            single<GroupService> { CommonGroupService(this@CommonGradeway) }
            single<RoleService> { CommonRoleService(this@CommonGradeway) }
        }

        val managerModule = module {
            single<ConfirmationManager> { CommonConfirmationManager() }
            // createdAtStart: the migrate command looks strategies up in MigrationStrategyRegistry directly,
            // before ever touching gradeway.migrations, so the registrations CommonMigrationManager's init
            // block performs must already have happened - a lazily created single would run them too late.
            single<MigrationManager>(createdAtStart = true) { CommonMigrationManager(this@CommonGradeway) }
            single<MessagingManager> { CommonMessagingManager(this@CommonGradeway) }
            single<DatabaseManager> { CommonDatabaseManager(this@CommonGradeway) }
            single<LanguageManager> { CommonLanguageManager(this@CommonGradeway) }
            single<DriverManager> { CommonDriverManager(this@CommonGradeway) }
            single<ConfigManager<TPlatformConfig>> { CommonConfigManager(this@CommonGradeway) }
            single<BackupManager> { CommonBackupManager(this@CommonGradeway) }
        }

        val commonModule = module {
            single<Caches> { CommonCaches(this@CommonGradeway) }
            single<Gradeway<TPlatformConfig>> { this@CommonGradeway }
        }

        if (!directory.exists()) {
            directory.mkdirs()
        }

        // GlobalContext is fine here: each platform plugin gets its own ClassLoader, so this shaded,
        // non-relocated Koin copy has private static state, isolated from other plugins on the server.
        startKoin {
            modules(serviceModule, managerModule, commonModule)
        }

        configs.load().onLeft { raise(it) }
        drivers.load().onLeft { raise(it) }
        languages.load().onLeft { raise(it) }
        messaging.load().onLeft { raise(it) }
        confirmations.load().onLeft { raise(it) }

        state = GradewayState.LOADED
    }.onLeft {
        state = GradewayState.UNLOADED
    }

    override fun unload(): Either<Throwable, Unit> = either {
        if (!state.allowUnload) raise(GradewayAlreadyUnloadedThrowable())
        state = GradewayState.PROCESSING

        caches.invalidateAll()
        backgroundScope.cancel()

        confirmations.unload().onLeft { raise(it) }
        messaging.unload().onLeft { raise(it) }
        languages.unload().onLeft { raise(it) }
        drivers.unload().onLeft { raise(it) }

        stopKoin()

        state = GradewayState.UNLOADED
    }.onLeft {
        state = GradewayState.LOADED
    }

    override fun reload(): Either<Throwable, Unit> = either {
        checkIsLoaded()

        configs.load().onLeft { raise(it) }
        messaging.reload().onLeft { raise(it) }
        languages.reload().onLeft { raise(it) }
    }

    override fun enable(): Either<Throwable, Unit> = either {
        checkIsLoaded()

        databases.enable().onLeft { raise(it) }
        messaging.enable().onLeft { raise(it) }

        caches.suggestions.initialize()

        val expireRolesJobIntervalSeconds =
            maxOf(configs.config.sweep.expiredRoleSweepIntervalSeconds, MIN_EXPIRED_ROLE_SWEEP_INTERVAL_SECONDS)
        expiredRoleSweepJob = backgroundScope.launch {
            while (isActive) {
                @Suppress("MagicNumber")
                delay((expireRolesJobIntervalSeconds * 1000).milliseconds)
                players.removeExpiredRoles().onLeft {
                    logger.warn("Failed to sweep expired player roles: $it")
                }
            }
        }
    }

    override fun disable(): Either<Throwable, Unit> = either {
        checkIsLoaded()

        expiredRoleSweepJob?.cancel()
        expiredRoleSweepJob = null

        databases.disable().onLeft { raise(it) }
        messaging.disable().onLeft { raise(it) }
        confirmations.disable().onLeft { raise(it) }
    }

    private fun Raise<Throwable>.checkIsLoaded() {
        if (state != GradewayState.LOADED) {
            raise(GradewayNotLoadedThrowable())
        }
    }

    companion object {
        private const val MIN_EXPIRED_ROLE_SWEEP_INTERVAL_SECONDS: Long = 60
    }
}
