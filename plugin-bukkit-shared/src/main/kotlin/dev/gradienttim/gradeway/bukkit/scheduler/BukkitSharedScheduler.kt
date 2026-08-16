/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.bukkit.scheduler

import org.bukkit.plugin.java.JavaPlugin

class BukkitSharedScheduler(val plugin: JavaPlugin) : SharedScheduler {
    override fun runTask(action: () -> Unit) {
        plugin.server.scheduler.runTask(plugin, Runnable(action))
    }
}
