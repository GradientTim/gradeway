/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.paper

import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.commands.createGradewayCommand
import dev.gradienttim.gradeway.driver.meta.DriverType
import dev.gradienttim.gradeway.paper.command.PaperAudienceProvider
import dev.gradienttim.gradeway.paper.listeners.ConnectionListener
import dev.gradienttim.gradeway.paper.messaging.PaperPluginMessageDriver
import dev.gradienttim.gradeway.paper.permission.GradewayPermissibleBase
import dev.gradienttim.gradeway.platform.CommonLogger
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.incendo.cloud.execution.ExecutionCoordinator
import org.incendo.cloud.paper.PaperCommandManager
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles

class GradewayPlugin : JavaPlugin() {
    private var entityPermissionHandle: MethodHandle? = null

    val gradeway = CommonGradeway(
        logger = CommonLogger.fromSlf4jLogger(slF4JLogger),
        directory = dataFolder,
    )

    override fun onEnable() {
        initializeEntityPermissionHandle()

        gradeway.load()
            .onLeft { throwable ->
                slF4JLogger.error("Failed to load Gradeway: ${throwable.message}")
                server.pluginManager.disablePlugin(this)
            }
            .onRight {
                gradeway.drivers.registerDriver(
                    id = "plugin-message",
                    type = DriverType.MESSAGING,
                    driver = PaperPluginMessageDriver(this)
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
        server.pluginManager.registerEvents(ConnectionListener(this), this)
    }

    fun applyEntityPermissions(player: Player) {
        entityPermissionHandle?.invoke(player, GradewayPermissibleBase(gradeway, player))
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

    private fun initializeEntityPermissionHandle() {
        try {
            val craftHumanEntityClass = Class.forName("org.bukkit.craftbukkit.entity.CraftHumanEntity")
            val permissionField = craftHumanEntityClass.getDeclaredField("perm")
            permissionField.isAccessible = true
            entityPermissionHandle = MethodHandles.lookup().unreflectSetter(permissionField)
        } catch (throwable: Throwable) {
            slF4JLogger.error(throwable.localizedMessage, throwable)
        }
    }
}
