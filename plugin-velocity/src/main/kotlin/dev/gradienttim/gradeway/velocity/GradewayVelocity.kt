/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.velocity

import com.google.inject.Inject
import com.velocitypowered.api.command.BrigadierCommand
import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import dev.gradienttim.gradeway.BuildInfo
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.commands.gradewayCommandBuilder
import dev.gradienttim.gradeway.driver.meta.DriverType
import dev.gradienttim.gradeway.velocity.listeners.ConnectionListener
import dev.gradienttim.gradeway.velocity.listeners.PermissionListener
import dev.gradienttim.gradeway.velocity.messaging.VelocityPluginMessageDriver
import dev.gradienttim.gradeway.platform.CommonLogger
import org.slf4j.Logger
import java.nio.file.Path

@Plugin(
    id = "gradeway",
    name = "Gradeway",
    authors = ["GradientTim"],
    version = BuildInfo.VERSION,
)
class GradewayVelocity @Inject constructor(
    val server: ProxyServer,
    val logger: Logger,
    @DataDirectory val dataDirectory: Path,
) {
    val gradeway = CommonGradeway(
        logger = CommonLogger.fromSlf4jLogger(logger),
        directory = dataDirectory.toFile(),
    )

    @Subscribe
    @Suppress("UnusedParameter")
    fun onProxyInitialize(event: ProxyInitializeEvent) {
        gradeway.load()
            .onLeft {
                logger.error("Failed to load Gradeway: ${it.message}")
            }
            .onRight {
                gradeway.drivers.registerDriver(
                    id = "plugin-message",
                    type = DriverType.MESSAGING,
                    driver = VelocityPluginMessageDriver(server, this)
                )

                gradeway.enable()
                    .onLeft { throwable ->
                        logger.error("Failed to enable Gradeway: ${throwable.message}")
                    }
                    .onRight {
                        registerEvents()
                        registerCommands()
                    }
            }
    }

    @Subscribe
    @Suppress("UnusedParameter")
    fun onProxyShutdown(event: ProxyShutdownEvent) {
        gradeway.unload()
    }

    private fun registerEvents() {
        server.eventManager.register(this, ConnectionListener(gradeway))
        server.eventManager.register(this, PermissionListener(gradeway))
    }

    private fun registerCommands() {
        registerGradewayCommand()
    }

    private fun registerGradewayCommand() {
        val gradewayCommandMeta = server.commandManager.metaBuilder("gradewayvelocity")
            .plugin(this)
            .aliases("gradewayv", "gwvelocity", "gwv")
            .build()

        val gradewayCommand = gradewayCommandBuilder<CommandSource>(
            gradeway = gradeway,
            literal = "gradewayvelocity",
            hasPermission = { source, permission ->
                source.permissionChecker.value(permission).toBooleanOrElse(false)
            },
            sourceToAudience = { source -> source },
        ).build()

        server.commandManager.register(gradewayCommandMeta, BrigadierCommand(gradewayCommand))
    }
}
