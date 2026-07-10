/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.bukkit

import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.bukkit.listeners.ConnectionListener
import dev.gradienttim.gradeway.bukkit.messaging.BukkitPluginMessageDriver
import dev.gradienttim.gradeway.bukkit.permission.GradewayPermissibleBase
import dev.gradienttim.gradeway.commands.gradewayCommandBuilder
import dev.gradienttim.gradeway.driver.meta.DriverType
import dev.gradienttim.gradeway.platform.CommonLogger
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
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
                    driver = BukkitPluginMessageDriver(this)
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
        lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) {
            val registrar = it.registrar()
            registerGradewayCommand(registrar)
        }
    }

    private fun registerGradewayCommand(registrar: Commands) {
        val gradewayCommand = gradewayCommandBuilder<CommandSourceStack>(
            gradeway = gradeway,
            literal = "gradeway",
            hasPermission = { source, permission -> source.sender.hasPermission(permission) },
            sourceToAudience = { source -> source.sender },
        ).build()

        registrar.register(gradewayCommand, listOf("gw", "gradewayb", "gwbukkit", "gwb"))
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
