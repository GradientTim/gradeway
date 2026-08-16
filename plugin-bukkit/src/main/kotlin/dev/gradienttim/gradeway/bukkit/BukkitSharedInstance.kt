/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.bukkit

import dev.gradienttim.gradeway.bukkit.scheduler.BukkitSharedScheduler
import dev.gradienttim.gradeway.bukkit.scheduler.SharedScheduler
import org.bukkit.Server
import org.bukkit.plugin.java.JavaPlugin

class BukkitSharedInstance(plugin: JavaPlugin) : SharedInstance {
    override val server: Server = plugin.server
    override val scheduler: SharedScheduler = BukkitSharedScheduler(plugin)
}
