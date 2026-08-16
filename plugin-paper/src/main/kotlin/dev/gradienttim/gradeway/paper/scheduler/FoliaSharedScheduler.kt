/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.paper.scheduler

import dev.gradienttim.gradeway.bukkit.scheduler.SharedScheduler
import org.bukkit.plugin.java.JavaPlugin

class FoliaSharedScheduler(val plugin: JavaPlugin) : SharedScheduler {
    override fun runTask(action: () -> Unit) {
        plugin.server.globalRegionScheduler.execute(plugin, Runnable(action))
    }
}
