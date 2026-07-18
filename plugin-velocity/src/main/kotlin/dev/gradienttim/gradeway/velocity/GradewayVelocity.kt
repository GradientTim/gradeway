/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.velocity

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import dev.gradienttim.gradeway.BuildInfo
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.commands.createGradewayCommand
import dev.gradienttim.gradeway.driver.meta.DriverType
import dev.gradienttim.gradeway.platform.CommonLogger
import dev.gradienttim.gradeway.velocity.command.VelocityAudienceProvider
import dev.gradienttim.gradeway.velocity.listeners.ConnectionListener
import dev.gradienttim.gradeway.velocity.listeners.PermissionListener
import dev.gradienttim.gradeway.velocity.messaging.VelocityPluginMessageDriver
import org.incendo.cloud.SenderMapper
import org.incendo.cloud.execution.ExecutionCoordinator
import org.incendo.cloud.velocity.VelocityCommandManager
import org.slf4j.Logger
import java.nio.file.Path
import kotlin.jvm.optionals.getOrNull

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
        val pluginContainer = server.pluginManager.getPlugin("gradeway").getOrNull()
            ?: error("Unable to get PluginContainer from Gradeway.")

        val audienceProvider = VelocityAudienceProvider()
        val commandManager = VelocityCommandManager(
            pluginContainer,
            server,
            ExecutionCoordinator.simpleCoordinator(),
            SenderMapper.identity()
        )

        createGradewayCommand(
            literal = "gradewayvelocity",
            aliases = arrayOf("gradewayv", "gwvelocity", "gwv"),
            gradeway = gradeway,
            commandManager = commandManager,
            audienceProvider = audienceProvider
        )
    }
}
