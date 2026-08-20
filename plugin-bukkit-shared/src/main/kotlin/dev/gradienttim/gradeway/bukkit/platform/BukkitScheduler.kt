/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.bukkit.platform

import dev.gradienttim.gradeway.bukkit.extensions.toTicks
import dev.gradienttim.gradeway.platform.Scheduler
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import java.util.concurrent.TimeUnit

class BukkitScheduler(val plugin: JavaPlugin) : Scheduler {
    override fun runTask(task: () -> Unit) {
        plugin.server.scheduler.runTask(plugin, task)
    }

    override fun runTaskLater(delay: Long, unit: TimeUnit, task: () -> Unit): Scheduler.Task {
        val bukkitTask = plugin.server.scheduler.runTaskLater(plugin, task, delay.toTicks(unit))
        return BukkitGradewayTask(bukkitTask)
    }

    override fun runTaskTimer(
        delay: Long,
        delayUnit: TimeUnit,
        interval: Long,
        intervalUnit: TimeUnit,
        task: () -> Unit
    ): Scheduler.Task {
        val bukkitTask = plugin.server.scheduler.runTaskTimer(
            plugin,
            task,
            delay.toTicks(delayUnit),
            interval.toTicks(intervalUnit)
        )

        return BukkitGradewayTask(bukkitTask)
    }

    class BukkitGradewayTask(val task: BukkitTask) : Scheduler.Task {
        override fun cancel(): Boolean {
            if (task.isCancelled) return false
            task.cancel()
            return true
        }
    }
}
