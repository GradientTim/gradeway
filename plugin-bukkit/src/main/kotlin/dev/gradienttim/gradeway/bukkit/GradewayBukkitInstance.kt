/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.bukkit

import com.mojang.brigadier.CommandDispatcher
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.bukkit.command.BukkitBrigadierCommand
import dev.gradienttim.gradeway.bukkit.command.context.BukkitCommandContext
import dev.gradienttim.gradeway.bukkit.config.BukkitPlatformConfig
import dev.gradienttim.gradeway.bukkit.listeners.ConnectionListener
import dev.gradienttim.gradeway.bukkit.listeners.PluginLifecycleListener
import dev.gradienttim.gradeway.bukkit.messaging.PluginMessageDriver
import dev.gradienttim.gradeway.bukkit.notification.SpigotIngameNotificationPlatform
import dev.gradienttim.gradeway.bukkit.platform.BukkitKnownPermissionSource
import dev.gradienttim.gradeway.bukkit.platform.BukkitScheduler
import dev.gradienttim.gradeway.bukkit.platform.BukkitStores
import dev.gradienttim.gradeway.commands.createGradewayCommand
import dev.gradienttim.gradeway.driver.meta.DriverType
import dev.gradienttim.gradeway.notification.drivers.ingame.IngameNotificationDriver
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
    private lateinit var notificationPlatform: SpigotIngameNotificationPlatform

    fun initialize() {
        if (::gradeway.isInitialized) return

        gradeway = CommonGradeway(
            logger = CommonLogger.fromJavaLogger(logger),
            stores = BukkitStores(
                persistentNotifications = { gradeway.configs.platformEntry.config.notification.persistent },
            ),
            scheduler = BukkitScheduler(plugin),
            directory = directory,
            defaultPlatformConfig = BukkitPlatformConfig(),
            platformConfigSerializer = BukkitPlatformConfig.serializer(),
            knownPermissionSource = BukkitKnownPermissionSource(plugin.server),
        )

        gradeway.load()
            .onLeft { throwable ->
                logger.severe("Failed to load Gradeway: ${throwable.message}")
            }
            .onRight {
                notificationPlatform = SpigotIngameNotificationPlatform(plugin.server, BukkitCommandContext(gradeway))

                gradeway.drivers.registerDriver(
                    id = "plugin-message",
                    type = DriverType.MESSAGING,
                    driver = PluginMessageDriver(plugin)
                )

                gradeway.drivers.registerDriver(
                    id = IngameNotificationDriver.ID,
                    type = DriverType.NOTIFICATION,
                    driver = IngameNotificationDriver(notificationPlatform)
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
        val connectionListener = ConnectionListener(plugin.server, gradeway, notificationPlatform)
        plugin.server.pluginManager.registerEvents(connectionListener, plugin)
        plugin.server.pluginManager.registerEvents(PluginLifecycleListener(gradeway), plugin)
    }

    private fun registerCommands() {
        val commandContext = BukkitCommandContext(gradeway)

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
