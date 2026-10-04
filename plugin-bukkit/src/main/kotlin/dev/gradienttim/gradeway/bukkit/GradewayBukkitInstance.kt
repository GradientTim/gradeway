/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.bukkit

import com.mojang.brigadier.CommandDispatcher
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.bukkit.command.BukkitBrigadierCommand
import dev.gradienttim.gradeway.bukkit.command.BukkitCommandContext
import dev.gradienttim.gradeway.bukkit.config.BukkitPlatformConfig
import dev.gradienttim.gradeway.bukkit.listeners.ConnectionListener
import dev.gradienttim.gradeway.bukkit.messaging.PluginMessageDriver
import dev.gradienttim.gradeway.bukkit.platform.BukkitScheduler
import dev.gradienttim.gradeway.commands.createGradewayCommand
import dev.gradienttim.gradeway.driver.meta.DriverType
import dev.gradienttim.gradeway.platform.CommonLogger
import org.bukkit.command.CommandSender
import java.nio.file.Path
import java.util.logging.Logger

class GradewayBukkitInstance(
    val plugin: GradewayPlugin,
    val logger: Logger,
    val directory: Path
) {
    private val commandDispatcher = CommandDispatcher<CommandSender>()
    private lateinit var gradeway: CommonGradeway<BukkitPlatformConfig>

    fun initialize() {
        if (::gradeway.isInitialized) return

        gradeway = CommonGradeway(
            logger = CommonLogger.fromJavaLogger(logger),
            scheduler = BukkitScheduler(plugin),
            directory = directory,
            defaultPlatformConfig = BukkitPlatformConfig(),
            platformConfigSerializer = BukkitPlatformConfig.serializer(),
        )

        gradeway.load()
            .onLeft { throwable ->
                logger.severe("Failed to load Gradeway: ${throwable.message}")
            }
            .onRight {
                gradeway.drivers.registerDriver(
                    id = "plugin-message",
                    type = DriverType.MESSAGING,
                    driver = PluginMessageDriver(plugin)
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

    fun terminate() {
        if (!::gradeway.isInitialized) return

        gradeway.disable()
            .onLeft { logger.severe("Failed to disable Gradeway: ${it.message}") }
            .onRight {
                gradeway.unload()
                    .onLeft { logger.severe("Failed to unload Gradeway: ${it.message}") }
            }
    }

    private fun registerEvents() {
        plugin.server.pluginManager.registerEvents(ConnectionListener(plugin.server, gradeway), plugin)
    }

    private fun registerCommands() {
        val commandContext = BukkitCommandContext()

        registerGradewayCommand(commandContext)
    }

    private fun registerGradewayCommand(context: BukkitCommandContext) {
        val gradewayCommand = createGradewayCommand(
            literal = "gradeway",
            gradeway = gradeway,
            commandContext = context,
        )

        val command = BukkitBrigadierCommand(
            dispatcher = commandDispatcher,
            context = context,
            builder = gradewayCommand,
        )

        val pluginCommand = plugin.getCommand("gradeway") ?: run {
            logger.severe("Command 'gradeway' is not declared in plugin.yml")
            return
        }

        pluginCommand.setExecutor(command)
        pluginCommand.tabCompleter = command
    }
}
