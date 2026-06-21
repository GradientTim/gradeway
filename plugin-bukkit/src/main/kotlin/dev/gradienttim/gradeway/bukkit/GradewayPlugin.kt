/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.bukkit

import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.bukkit.listeners.ConnectionListener
import dev.gradienttim.gradeway.commands.gradewayCommandBuilder
import dev.gradienttim.gradeway.platform.CommonLogger
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import org.bukkit.entity.Player
import org.bukkit.permissions.PermissionAttachment
import org.bukkit.plugin.java.JavaPlugin
import java.util.*

class GradewayPlugin : JavaPlugin() {
    val gradeway = CommonGradeway(
        logger = CommonLogger.fromSlf4jLogger(slF4JLogger),
        directory = dataFolder.toPath()
    )

    internal val attachments = mutableMapOf<UUID, PermissionAttachment>()

    override fun onEnable() {
        gradeway.load()
            .onLeft { throwable ->
                slF4JLogger.error("Failed to load Gradeway: ${throwable.localizedMessage}")
                server.pluginManager.disablePlugin(this)
            }
            .onRight {
                registerEvents()
                registerCommands()
            }
    }

    override fun onDisable() {
        gradeway.unload()
    }

    fun registerPermissionAttachment(player: Player) {
        val uniqueId = player.uniqueId
        if (attachments.containsKey(uniqueId)) {
            return
        }
        val attachment = player.addAttachment(this)
        attachments[uniqueId] = attachment
        applyPermissionsToAttachment(uniqueId, attachment)
    }

    fun unregisterPermissionAttachment(player: Player) {
        val uniqueId = player.uniqueId
        if (attachments.containsKey(uniqueId)) {
            attachments.remove(uniqueId)
        }
    }

    fun updatePermissionAttachment(player: Player) {
        val uniqueId = player.uniqueId
        if (!attachments.containsKey(uniqueId)) {
            registerPermissionAttachment(player)
            return
        }
        val attachment = attachments[uniqueId]!!
        applyPermissionsToAttachment(uniqueId, attachment)
    }

    fun applyPermissionsToAttachment(id: UUID, attachment: PermissionAttachment) {
        val permissions = gradeway.permissions.getPlayerPermissions(id)
        for ((permission, enabled) in permissions) {
            attachment.setPermission(permission, enabled)
        }
    }

    private fun registerEvents() {
        server.pluginManager.registerEvents(ConnectionListener(this), this)
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
}
