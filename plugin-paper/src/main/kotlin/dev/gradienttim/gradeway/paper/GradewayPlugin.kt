/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.paper

import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.bukkit.config.BukkitPlatformConfig
import dev.gradienttim.gradeway.bukkit.listeners.ConnectionListener
import dev.gradienttim.gradeway.bukkit.messaging.PluginMessageDriver
import dev.gradienttim.gradeway.commands.createGradewayCommand
import dev.gradienttim.gradeway.driver.meta.DriverType
import dev.gradienttim.gradeway.paper.command.PaperAudienceProvider
import dev.gradienttim.gradeway.platform.CommonLogger
import org.bukkit.plugin.java.JavaPlugin
import org.incendo.cloud.execution.ExecutionCoordinator
import org.incendo.cloud.paper.PaperCommandManager

class GradewayPlugin : JavaPlugin() {
    val gradeway = CommonGradeway(
        logger = CommonLogger.fromSlf4jLogger(slF4JLogger),
        directory = dataFolder,
        defaultPlatformConfig = BukkitPlatformConfig(),
    )

    override fun onEnable() {
        gradeway.load()
            .onLeft { throwable ->
                slF4JLogger.error("Failed to load Gradeway: ${throwable.message}")
                server.pluginManager.disablePlugin(this)
            }
            .onRight {
                gradeway.drivers.registerDriver(
                    id = "plugin-message",
                    type = DriverType.MESSAGING,
                    driver = PluginMessageDriver(this)
                )

                gradeway.enable()
                    .onLeft { throwable ->
                        slF4JLogger.error("Failed to enable Gradeway: ${throwable.message}")
                    }
                    .onRight {
                        registerEvents()
                        registerCommands()
                    }
            }
    }

    override fun onDisable() {
        gradeway.disable()
            .onLeft { slF4JLogger.error("Failed to disable Gradeway: ${it.message}") }
            .onRight {
                gradeway.unload()
                    .onLeft { slF4JLogger.error("Failed to unload Gradeway: ${it.message}") }
            }
    }

    private fun registerEvents() {
        server.pluginManager.registerEvents(ConnectionListener(gradeway), this)
    }

    private fun registerCommands() {
        val audienceProvider = PaperAudienceProvider()
        val commandManager = PaperCommandManager.builder()
            .executionCoordinator(ExecutionCoordinator.simpleCoordinator())
            .buildOnEnable(this)

        createGradewayCommand(
            literal = "gradeway",
            aliases = arrayOf("gw", "gradewayp", "gwpaper", "gwp"),
            gradeway = gradeway,
            commandManager = commandManager,
            audienceProvider = audienceProvider
        )
    }
}
