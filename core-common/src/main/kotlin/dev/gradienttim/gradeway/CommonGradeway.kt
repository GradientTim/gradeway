/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.either
import dev.gradienttim.gradeway.managers.*
import dev.gradienttim.gradeway.platform.CommonEnvironment
import dev.gradienttim.gradeway.platform.Logger
import dev.gradienttim.gradeway.services.*
import dev.gradienttim.gradeway.throwables.GradewayAlreadyLoadedThrowable
import dev.gradienttim.gradeway.throwables.GradewayAlreadyUnloadedThrowable
import dev.gradienttim.gradeway.throwables.GradewayNotLoadedThrowable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import net.kyori.adventure.text.minimessage.MiniMessage
import org.jetbrains.exposed.v1.jdbc.Database
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.io.File
import java.time.Instant

class CommonGradeway(
    override val logger: Logger,
    override val directory: File,
) : GradewayLifecycle, KoinComponent {
    override val now: () -> Instant = { Instant.now() }
    override var backgroundScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override val permissions: PermissionService by inject()
    override val attributes: AttributeService by inject()
    override val players: PlayerService by inject()
    override val groups: GroupService by inject()
    override val roles: RoleService by inject()

    override val databases: DatabaseManager by inject()
    override val languages: LanguageManager by inject()
    override val messaging: MessagingManager by inject()
    override val drivers: DriverManager by inject()
    override val configs: ConfigManager by inject()
    override val backups: BackupManager by inject()

    override val environment by lazy { CommonEnvironment(this) }
    override var state: GradewayState = GradewayState.UNLOADED

    internal lateinit var miniMessage: MiniMessage
    internal lateinit var database: Database

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
            single<DatabaseManager> { CommonDatabaseManager(this@CommonGradeway) }
            single<LanguageManager> { CommonLanguageManager(this@CommonGradeway) }
            single<MessagingManager> { CommonMessagingManager(this@CommonGradeway) }
            single<DriverManager> { CommonDriverManager(this@CommonGradeway) }
            single<ConfigManager> { CommonConfigManager(this@CommonGradeway) }
            single<BackupManager> { CommonBackupManager(this@CommonGradeway) }
        }

        val commonModule = module {
            single<Gradeway> { this@CommonGradeway }
        }

        if (!directory.exists()) {
            directory.mkdirs()
        }

        startKoin {
            modules(serviceModule, managerModule, commonModule)
        }

        configs.load().onLeft { raise(it) }
        drivers.load().onLeft { raise(it) }
        languages.load().onLeft { raise(it) }
        messaging.load().onLeft { raise(it) }

        state = GradewayState.LOADED
    }.onLeft {
        state = GradewayState.UNLOADED
    }

    override fun unload(): Either<Throwable, Unit> = either {
        if (!state.allowUnload) raise(GradewayAlreadyUnloadedThrowable())
        state = GradewayState.PROCESSING

        backgroundScope.cancel()

        messaging.unload().onLeft { raise(it) }
        languages.unload().onLeft { raise(it) }
        drivers.unload().onLeft { raise(it) }

        // stopKoin() (not koin.close()) - it also closes the underlying KoinApplication but
        // additionally deregisters it from Koin's global context. Without that, a later load()
        // call in the same JVM (e.g., a plugin disable/enable cycle) fails with KoinApplicationAlreadyStartedException.
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
    }

    override fun disable(): Either<Throwable, Unit> = either {
        checkIsLoaded()

        databases.disable().onLeft { raise(it) }
        messaging.disable().onLeft { raise(it) }
    }

    private fun Raise<Throwable>.checkIsLoaded() {
        if (state != GradewayState.LOADED) {
            raise(GradewayNotLoadedThrowable())
        }
    }
}
