/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.paper.platform

import dev.gradienttim.gradeway.bukkit.extensions.toTicks
import dev.gradienttim.gradeway.platform.Scheduler
import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import org.bukkit.plugin.java.JavaPlugin
import java.util.concurrent.TimeUnit

class FoliaScheduler(val plugin: JavaPlugin) : Scheduler {
    override fun runTask(task: () -> Unit) {
        plugin.server.globalRegionScheduler.execute(plugin, task)
    }

    override fun runTaskLater(delay: Long, unit: TimeUnit, task: () -> Unit): Scheduler.Task {
        val scheduledTask = plugin.server.globalRegionScheduler.runDelayed(
            plugin,
            { task() },
            delay.toTicks(unit)
        )
        return FoliaGradewayTask(scheduledTask)
    }

    override fun runTaskTimer(
        delay: Long,
        delayUnit: TimeUnit,
        interval: Long,
        intervalUnit: TimeUnit,
        task: () -> Unit
    ): Scheduler.Task {
        val scheduledTask = plugin.server.globalRegionScheduler.runAtFixedRate(
            plugin,
            { task() },
            delay.toTicks(delayUnit),
            interval.toTicks(intervalUnit)
        )
        return FoliaGradewayTask(scheduledTask)
    }

    class FoliaGradewayTask(val task: ScheduledTask) : Scheduler.Task {
        override fun cancel(): Boolean {
            val state = task.cancel()
            return state == ScheduledTask.CancelledState.CANCELLED_BY_CALLER
        }
    }
}
