/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.paper

import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.bukkit.config.BukkitPlatformConfig
import dev.gradienttim.gradeway.bukkit.listeners.ConnectionListener
import dev.gradienttim.gradeway.bukkit.messaging.PluginMessageDriver
import dev.gradienttim.gradeway.bukkit.platform.BukkitScheduler
import dev.gradienttim.gradeway.commands.createGradewayCommand
import dev.gradienttim.gradeway.driver.meta.DriverType
import dev.gradienttim.gradeway.paper.command.context.PaperCommandContext
import dev.gradienttim.gradeway.paper.platform.FoliaScheduler
import dev.gradienttim.gradeway.platform.CommonLogger
import io.papermc.paper.ServerBuildInfo
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import net.kyori.adventure.key.Key
import org.bukkit.plugin.java.JavaPlugin

class GradewayPlugin : JavaPlugin() {
    val gradeway = CommonGradeway(
        logger = CommonLogger.fromSlf4jLogger(slF4JLogger),
        scheduler = if (isFolia()) FoliaScheduler(this) else BukkitScheduler(this),
        directory = dataFolder.toPath(),
        defaultPlatformConfig = BukkitPlatformConfig(),
        platformConfigSerializer = BukkitPlatformConfig.serializer(),
    )

    override fun onEnable() {
        gradeway.load()
            .onLeft { throwable ->
                slF4JLogger.error("Failed to load Gradeway: ${throwable.message}")
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
        server.pluginManager.registerEvents(ConnectionListener(server, gradeway), this)
    }

    private fun registerCommands() {
        val commandContext = PaperCommandContext()

        lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) {
            val commands = it.registrar()

            registerGradewayCommand(commands, commandContext)
        }
    }

    private fun registerGradewayCommand(
        commands: Commands,
        commandContext: PaperCommandContext,
    ) {
        val gradewayCommand = createGradewayCommand(
            literal = "gradeway",
            gradeway = gradeway,
            commandContext = commandContext,
        )

        commands.register(gradewayCommand.build(), listOf("gw", "gradewayp", "gwpaper", "gwp"))
    }

    // https://docs.papermc.io/paper/dev/folia-support/#checking-for-folia
    private fun isFolia(): Boolean {
        return ServerBuildInfo.buildInfo().isBrandCompatible(Key.key("papermc", "folia"))
    }
}
