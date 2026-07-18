/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.bungee

import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.bungee.command.BungeeAudienceProvider
import dev.gradienttim.gradeway.bungee.listeners.ConnectionListener
import dev.gradienttim.gradeway.bungee.listeners.PermissionListener
import dev.gradienttim.gradeway.bungee.messaging.PluginMessageDriver
import dev.gradienttim.gradeway.commands.createGradewayCommand
import dev.gradienttim.gradeway.driver.meta.DriverType
import dev.gradienttim.gradeway.platform.CommonLogger
import net.kyori.adventure.platform.bungeecord.BungeeAudiences
import net.md_5.bungee.api.plugin.Plugin
import org.incendo.cloud.SenderMapper
import org.incendo.cloud.bungee.BungeeCommandManager
import org.incendo.cloud.execution.ExecutionCoordinator

class GradewayPlugin : Plugin() {
    var adventure: BungeeAudiences? = null
        internal set

    val gradeway = CommonGradeway(
        logger = CommonLogger.fromJavaLogger(logger),
        directory = dataFolder,
    )

    override fun onEnable() {
        adventure = BungeeAudiences.create(this)

        gradeway.load()
            .onLeft { throwable ->
                logger.severe("Failed to load Gradeway: ${throwable.message}")
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
        proxy.pluginManager.registerListener(this, ConnectionListener(gradeway))
        proxy.pluginManager.registerListener(this, PermissionListener(gradeway))
    }

    private fun registerCommands() {
        val audienceProvider = BungeeAudienceProvider(this)
        val commandManager = BungeeCommandManager(
            this,
            ExecutionCoordinator.simpleCoordinator(),
            SenderMapper.identity()
        )

        createGradewayCommand(
            literal = "gradewaybungeecord",
            aliases = arrayOf("gradewaybc", "gwbungeecord", "gwbc", "gwbungee", "gradewaybungee"),
            gradeway = gradeway,
            commandManager = commandManager,
            audienceProvider = audienceProvider
        )
    }
}
