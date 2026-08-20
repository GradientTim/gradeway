/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.bungee.platform

import dev.gradienttim.gradeway.bungee.GradewayPlugin
import dev.gradienttim.gradeway.platform.Scheduler
import net.md_5.bungee.api.scheduler.ScheduledTask
import java.util.concurrent.TimeUnit

class BungeeCordScheduler(val plugin: GradewayPlugin) : Scheduler {
    override fun runTask(task: () -> Unit) {
        plugin.proxy.scheduler.runAsync(plugin, task)
    }

    override fun runTaskLater(delay: Long, unit: TimeUnit, task: () -> Unit): Scheduler.Task {
        val scheduledTask = plugin.proxy.scheduler.schedule(plugin, task, delay, unit)
        return BungeeCordGradewayTask(scheduledTask)
    }

    override fun runTaskTimer(
        delay: Long,
        delayUnit: TimeUnit,
        interval: Long,
        intervalUnit: TimeUnit,
        task: () -> Unit
    ): Scheduler.Task {
        // bungeecord uses one TimeUnit for delay and interval...
        val scheduledTask = plugin.proxy.scheduler.schedule(plugin, task, delay, interval, intervalUnit)
        return BungeeCordGradewayTask(scheduledTask)
    }

    class BungeeCordGradewayTask(val task: ScheduledTask) : Scheduler.Task {
        override fun cancel(): Boolean {
            task.cancel()
            return true
        }
    }
}
