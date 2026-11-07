/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.paper.platform

import dev.gradienttim.gradeway.bukkit.platform.BukkitKnownPermissionSource
import org.bukkit.Server

class PaperKnownPermissionSource(server: Server) : BukkitKnownPermissionSource(server) {
    override fun collect(): Collection<String> {
        val commands = server.commandMap.knownCommands.values.flatMap { splitCommandPermission(it.permission) }
        return super.collect() + commands
    }
}
