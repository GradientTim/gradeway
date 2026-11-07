/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.bukkit.platform

import dev.gradienttim.gradeway.platform.KnownPermissionSource
import org.bukkit.Server

open class BukkitKnownPermissionSource(protected val server: Server) : KnownPermissionSource {
    override fun collect(): Collection<String> {
        val registered = server.pluginManager.permissions.map { it.name }
        val commands = server.pluginManager.plugins.flatMap { plugin ->
            plugin.description.commands.values.flatMap { splitCommandPermission(it["permission"] as? String) }
        }

        return registered + commands
    }

    protected fun splitCommandPermission(permission: String?): List<String> =
        permission?.split(';')?.filter(String::isNotBlank).orEmpty()
}
