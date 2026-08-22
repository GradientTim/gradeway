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
import dev.gradienttim.gradeway.bukkit.platform.BukkitScheduler
import dev.gradienttim.gradeway.commands.createGradewayCommand
import dev.gradienttim.gradeway.driver.meta.DriverType
import dev.gradienttim.gradeway.platform.CommonLogger
import net.kyori.adventure.platform.bukkit.BukkitAudiences
import org.bukkit.command.CommandSender
import org.incendo.cloud.SenderMapper
import org.incendo.cloud.execution.ExecutionCoordinator
import org.incendo.cloud.minecraft.extras.AudienceProvider
import org.incendo.cloud.paper.LegacyPaperCommandManager
import java.io.File
import java.util.logging.Logger

class GradewayBukkitInstance(
    val plugin: GradewayPlugin,
    val logger: Logger,
    val directory: File
) {
    var adventure: BukkitAudiences? = null
    private lateinit var gradeway: CommonGradeway<BukkitPlatformConfig>

    fun initialize() {
        if (::gradeway.isInitialized) return

        adventure = BukkitAudiences.create(plugin)

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
        plugin.server.pluginManager.registerEvents(ConnectionListener(plugin.server, gradeway), plugin)
    }

    private fun registerCommands() {
        val audienceProvider = BukkitAudienceProvider(this)
        val commandManager = LegacyPaperCommandManager(
            plugin,
            ExecutionCoordinator.simpleCoordinator(),
            SenderMapper.identity()
        )

        registerGradewayCommand(audienceProvider, commandManager)
    }

    private fun registerGradewayCommand(
        audienceProvider: AudienceProvider<CommandSender>,
        commandManager: LegacyPaperCommandManager<CommandSender>
    ) {
        createGradewayCommand(
            literal = "gradeway",
            aliases = arrayOf("gw", "gradewayb", "gwbukkit", "gwb"),
            gradeway = gradeway,
            commandManager = commandManager,
            audienceProvider = audienceProvider
        )
    }
}
