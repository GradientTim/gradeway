/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.paper

import dev.gradienttim.gradeway.bukkit.SharedInstance
import dev.gradienttim.gradeway.bukkit.scheduler.BukkitSharedScheduler
import dev.gradienttim.gradeway.bukkit.scheduler.SharedScheduler
import dev.gradienttim.gradeway.paper.scheduler.FoliaSharedScheduler
import io.papermc.paper.ServerBuildInfo
import net.kyori.adventure.key.Key
import org.bukkit.Server
import org.bukkit.plugin.java.JavaPlugin

class PaperSharedInstance(plugin: JavaPlugin) : SharedInstance {
    override val server: Server = plugin.server
    override val scheduler: SharedScheduler = if (isFolia()) {
        FoliaSharedScheduler(plugin)
    } else {
        BukkitSharedScheduler(plugin)
    }

    // https://docs.papermc.io/paper/dev/folia-support/#checking-for-folia
    private fun isFolia(): Boolean {
        return ServerBuildInfo.buildInfo().isBrandCompatible(Key.key("papermc", "folia"))
    }
}
