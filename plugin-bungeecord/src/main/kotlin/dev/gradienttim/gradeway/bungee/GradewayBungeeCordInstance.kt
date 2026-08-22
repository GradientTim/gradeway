/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.bungee

import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.bungee.command.BungeeAudienceProvider
import dev.gradienttim.gradeway.bungee.config.BungeeCordPlatformConfig
import dev.gradienttim.gradeway.bungee.listeners.ConnectionListener
import dev.gradienttim.gradeway.bungee.listeners.PermissionListener
import dev.gradienttim.gradeway.bungee.messaging.PluginMessageDriver
import dev.gradienttim.gradeway.bungee.platform.BungeeCordScheduler
import dev.gradienttim.gradeway.commands.createGradewayCommand
import dev.gradienttim.gradeway.driver.meta.DriverType
import dev.gradienttim.gradeway.platform.CommonLogger
import net.kyori.adventure.platform.bungeecord.BungeeAudiences
import net.md_5.bungee.api.CommandSender
import org.incendo.cloud.SenderMapper
import org.incendo.cloud.bungee.BungeeCommandManager
import org.incendo.cloud.execution.ExecutionCoordinator
import org.incendo.cloud.minecraft.extras.AudienceProvider
import java.io.File
import java.util.logging.Logger

class GradewayBungeeCordInstance(
    val plugin: GradewayPlugin,
    val logger: Logger,
    val directory: File,
) {
    var adventure: BungeeAudiences? = null
    private lateinit var gradeway: CommonGradeway<BungeeCordPlatformConfig>

    fun initialize() {
        if (::gradeway.isInitialized) return

        adventure = BungeeAudiences.create(plugin)

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
        plugin.proxy.pluginManager.registerListener(plugin, ConnectionListener(gradeway))
        plugin.proxy.pluginManager.registerListener(plugin, PermissionListener(gradeway))
    }

    private fun registerCommands() {
        val audienceProvider = BungeeAudienceProvider(this)
        val commandManager = BungeeCommandManager(
            plugin,
            ExecutionCoordinator.simpleCoordinator(),
            SenderMapper.identity()
        )

        registerGradewayCommand(audienceProvider, commandManager)
    }

    private fun registerGradewayCommand(
        audienceProvider: AudienceProvider<CommandSender>,
        commandManager: BungeeCommandManager<CommandSender>
    ) {
        createGradewayCommand(
            literal = "gradewaybungeecord",
            aliases = arrayOf("gradewaybc", "gwbungeecord", "gwbc", "gwbungee", "gradewaybungee"),
            gradeway = gradeway,
            commandManager = commandManager,
            audienceProvider = audienceProvider
        )
    }
}
