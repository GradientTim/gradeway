/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.bungee

import com.mojang.brigadier.CommandDispatcher
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.bungee.command.BungeeBrigadierCommand
import dev.gradienttim.gradeway.bungee.command.BungeeCommandContext
import dev.gradienttim.gradeway.bungee.config.BungeeCordPlatformConfig
import dev.gradienttim.gradeway.bungee.listeners.ConnectionListener
import dev.gradienttim.gradeway.bungee.listeners.PermissionListener
import dev.gradienttim.gradeway.bungee.messaging.PluginMessageDriver
import dev.gradienttim.gradeway.bungee.platform.BungeeCordScheduler
import dev.gradienttim.gradeway.commands.createGradewayCommand
import dev.gradienttim.gradeway.driver.meta.DriverType
import dev.gradienttim.gradeway.platform.CommonLogger
import net.md_5.bungee.api.CommandSender
import java.nio.file.Path
import java.util.logging.Logger

class GradewayBungeeCordInstance(
    val plugin: GradewayPlugin,
    val logger: Logger,
    val directory: Path,
) {
    private val commandDispatcher = CommandDispatcher<CommandSender>()
    private lateinit var gradeway: CommonGradeway<BungeeCordPlatformConfig>

    fun initialize() {
        if (::gradeway.isInitialized) return

        gradeway = CommonGradeway(
            logger = CommonLogger.fromJavaLogger(logger),
            scheduler = BungeeCordScheduler(plugin),
            directory = directory,
            defaultPlatformConfig = BungeeCordPlatformConfig(),
            platformConfigSerializer = BungeeCordPlatformConfig.serializer(),
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
        plugin.proxy.pluginManager.registerListener(plugin, ConnectionListener(gradeway))
        plugin.proxy.pluginManager.registerListener(plugin, PermissionListener(gradeway))
    }

    private fun registerCommands() {
        val commandContext = BungeeCommandContext()

        registerGradewayCommand(commandContext)
    }

    private fun registerGradewayCommand(context: BungeeCommandContext) {
        val gradewayCommand = createGradewayCommand(
            literal = "gradewaybungeecord",
            gradeway = gradeway,
            commandContext = context,
        )

        BungeeBrigadierCommand(
            plugin = plugin,
            builder = gradewayCommand,
            dispatcher = commandDispatcher,
            context = context,
            commandAliases = arrayOf("gradewaybc", "gwbungeecord", "gwbc", "gwbungee", "gradewaybungee"),
        )
    }
}
