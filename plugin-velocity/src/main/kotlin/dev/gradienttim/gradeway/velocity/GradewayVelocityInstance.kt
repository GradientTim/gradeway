/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.velocity

import com.velocitypowered.api.command.BrigadierCommand
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.commands.createGradewayCommand
import dev.gradienttim.gradeway.driver.meta.DriverType
import dev.gradienttim.gradeway.platform.CommonLogger
import dev.gradienttim.gradeway.velocity.command.VelocityCommandContext
import dev.gradienttim.gradeway.velocity.config.VelocityPlatformConfig
import dev.gradienttim.gradeway.velocity.listeners.ConnectionListener
import dev.gradienttim.gradeway.velocity.listeners.PermissionListener
import dev.gradienttim.gradeway.velocity.messaging.VelocityPluginMessageDriver
import dev.gradienttim.gradeway.velocity.platform.VelocityScheduler
import org.slf4j.Logger
import java.nio.file.Path

class GradewayVelocityInstance(
    val plugin: GradewayPlugin,
    val logger: Logger,
    val directory: Path,
) {
    private lateinit var gradeway: CommonGradeway<VelocityPlatformConfig>

    fun initialize() {
        if (::gradeway.isInitialized) return

        gradeway = CommonGradeway(
            logger = CommonLogger.fromSlf4jLogger(logger),
            scheduler = VelocityScheduler(plugin),
            directory = directory,
            defaultPlatformConfig = VelocityPlatformConfig(),
            platformConfigSerializer = VelocityPlatformConfig.serializer(),
        )

        gradeway.load()
            .onLeft {
                logger.error("Failed to load Gradeway: ${it.message}")
            }
            .onRight {
                gradeway.drivers.registerDriver(
                    id = "plugin-message",
                    type = DriverType.MESSAGING,
                    driver = VelocityPluginMessageDriver(plugin.server, this)
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

    fun terminate() {
        if (!::gradeway.isInitialized) return

        gradeway.disable()
            .onLeft { logger.error("Failed to disable Gradeway: ${it.message}") }
            .onRight {
                gradeway.unload()
                    .onLeft { logger.error("Failed to unload Gradeway: ${it.message}") }
            }
    }

    private fun registerEvents() {
        plugin.server.eventManager.register(plugin, ConnectionListener(gradeway))
        plugin.server.eventManager.register(plugin, PermissionListener(gradeway))
    }

    private fun registerCommands() {
        val commandContext = VelocityCommandContext()

        registerGradewayCommand(commandContext)
    }

    private fun registerGradewayCommand(commandContext: VelocityCommandContext) {
        val gradewayCommand = createGradewayCommand(
            literal = "gradewayvelocity",
            gradeway = gradeway,
            commandContext = commandContext,
        )

        val gradewayCommandMeta = plugin.server.commandManager
            .metaBuilder("gradewayvelocity")
            .aliases("gradewayv", "gwvelocity", "gwv")
            .plugin(plugin)
            .build()

        plugin.server.commandManager.register(gradewayCommandMeta, BrigadierCommand(gradewayCommand))
    }
}
