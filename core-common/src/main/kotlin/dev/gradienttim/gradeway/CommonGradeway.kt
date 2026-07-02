/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway

import arrow.core.Either
import arrow.core.raise.either
import dev.gradienttim.gradeway.managers.*
import dev.gradienttim.gradeway.messaging.MessagingBroker
import dev.gradienttim.gradeway.platform.CommonEnvironment
import dev.gradienttim.gradeway.platform.Logger
import dev.gradienttim.gradeway.services.*
import dev.gradienttim.gradeway.throwables.GradewayAlreadyLoadedThrowable
import dev.gradienttim.gradeway.throwables.GradewayAlreadyUnloadedThrowable
import dev.gradienttim.gradeway.throwables.GradewayNotLoadedThrowable
import net.kyori.adventure.text.minimessage.MiniMessage
import org.jetbrains.exposed.v1.jdbc.Database
import org.koin.core.KoinApplication
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import org.koin.dsl.module
import java.io.File
import java.time.Instant

class CommonGradeway(
    override val logger: Logger,
    override val directory: File,
) : GradewayLifecycle, KoinComponent {
    override val now: () -> Instant = { Instant.now() }

    override val permissions: PermissionService by inject()
    override val attributes: AttributeService by inject()
    override val players: PlayerService by inject()
    override val roles: RoleService by inject()

    override val databases: DatabaseManager by inject()
    override val languages: LanguageManager by inject()
    override val messaging: MessagingManager by inject()
    override val drivers: DriverManager by inject()
    override val configs: ConfigManager by inject()

    override val environment by lazy { CommonEnvironment(this) }
    override var state: GradewayState = GradewayState.UNLOADED

    internal lateinit var koin: KoinApplication
    internal lateinit var miniMessage: MiniMessage

    internal lateinit var broker: MessagingBroker
    internal lateinit var database: Database

    override fun load(): Either<Throwable, Unit> = either {
        if (!state.allowLoad) raise(GradewayAlreadyLoadedThrowable())
        state = GradewayState.PROCESSING

        val serviceModule = module {
            single<PermissionService> { CommonPermissionService(this@CommonGradeway) }
            single<AttributeService> { CommonAttributeService(this@CommonGradeway) }
            single<PlayerService> { CommonPlayerService(this@CommonGradeway) }
            single<RoleService> { CommonRoleService(this@CommonGradeway) }
        }

        val managerModule = module {
            single<DatabaseManager> { CommonDatabaseManager(this@CommonGradeway) }
            single<LanguageManager> { CommonLanguageManager(this@CommonGradeway) }
            single<MessagingManager> { CommonMessagingManager(this@CommonGradeway) }
            single<DriverManager> { CommonDriverManager(this@CommonGradeway) }
            single<ConfigManager> { CommonConfigManager(this@CommonGradeway) }
        }

        val commonModule = module {
            single<Gradeway> { this@CommonGradeway }
        }

        if (!directory.exists()) {
            directory.mkdirs()
        }

        koin = startKoin {
            modules(serviceModule, managerModule, commonModule)
        }

        configs.load().onLeft { raise(it) }
        drivers.load().onLeft { raise(it) }
        messaging.load().onLeft { raise(it) }
        databases.load().onLeft { raise(it) }
        languages.load().onLeft { raise(it) }

        state = GradewayState.LOADED
    }.onLeft {
        state = GradewayState.UNLOADED
    }

    override fun unload(): Either<Throwable, Unit> = either {
        if (!state.allowUnload) raise(GradewayAlreadyUnloadedThrowable())
        state = GradewayState.PROCESSING

        languages.unload().onLeft { raise(it) }
        databases.unload().onLeft { raise(it) }
        messaging.unload().onLeft { raise(it) }
        drivers.unload().onLeft { raise(it) }
        koin.close()

        state = GradewayState.UNLOADED
    }.onLeft {
        state = GradewayState.LOADED
    }

    override fun reload(): Either<Throwable, Unit> = either {
        if (state != GradewayState.LOADED) raise(GradewayNotLoadedThrowable())

        configs.load().onLeft { raise(it) }
        messaging.reload().onLeft { raise(it) }
        languages.reload().onLeft { raise(it) }
    }
}
