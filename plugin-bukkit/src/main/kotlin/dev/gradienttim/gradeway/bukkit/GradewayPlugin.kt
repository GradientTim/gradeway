/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.bukkit

import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.bukkit.command.BukkitAudienceProvider
import dev.gradienttim.gradeway.bukkit.config.BukkitPlatformConfig
import dev.gradienttim.gradeway.bukkit.listeners.ConnectionListener
import dev.gradienttim.gradeway.bukkit.messaging.PluginMessageDriver
import dev.gradienttim.gradeway.commands.createGradewayCommand
import dev.gradienttim.gradeway.driver.meta.DriverType
import dev.gradienttim.gradeway.platform.CommonLogger
import net.kyori.adventure.platform.bukkit.BukkitAudiences
import org.bukkit.plugin.java.JavaPlugin
import org.incendo.cloud.execution.ExecutionCoordinator
import org.incendo.cloud.paper.LegacyPaperCommandManager

class GradewayPlugin : JavaPlugin() {
    var adventure: BukkitAudiences? = null
        internal set

    val gradeway = CommonGradeway(
        logger = CommonLogger.fromJavaLogger(logger),
        directory = dataFolder,
        defaultPlatformConfig = BukkitPlatformConfig(),
    )

    override fun onEnable() {
        adventure = BukkitAudiences.create(this)

        gradeway.load()
            .onLeft { throwable ->
                logger.severe("Failed to load Gradeway: ${throwable.message}")
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
                        logger.severe("Failed to enable Gradeway: ${throwable.message}")
                    }
                    .onRight {
                        registerEvents()
                        registerCommands()
                    }
            }
    }

    override fun onDisable() {
        adventure?.close()
        adventure = null

        gradeway.disable()
            .onLeft { logger.severe("Failed to disable Gradeway: ${it.message}") }
            .onRight {
                gradeway.unload()
                    .onLeft { logger.severe("Failed to unload Gradeway: ${it.message}") }
            }
    }

    private fun registerEvents() {
        server.pluginManager.registerEvents(ConnectionListener(gradeway), this)
    }

    private fun registerCommands() {
        val audienceProvider = BukkitAudienceProvider(this)
        val commandManager = LegacyPaperCommandManager.createNative(
            this,
            ExecutionCoordinator.simpleCoordinator()
        )

        createGradewayCommand(
            literal = "gradeway",
            aliases = arrayOf("gw", "gradewayb", "gwbukkit", "gwb"),
            gradeway = gradeway,
            commandManager = commandManager,
            audienceProvider = audienceProvider
        )
    }
}
