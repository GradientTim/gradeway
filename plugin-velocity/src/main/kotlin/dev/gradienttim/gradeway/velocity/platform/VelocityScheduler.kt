/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.velocity.platform

import com.velocitypowered.api.scheduler.ScheduledTask
import com.velocitypowered.api.scheduler.TaskStatus
import dev.gradienttim.gradeway.platform.Scheduler
import dev.gradienttim.gradeway.velocity.GradewayVelocity
import java.util.concurrent.TimeUnit

class VelocityScheduler(val plugin: GradewayVelocity) : Scheduler {
    override fun runTask(task: () -> Unit) {
        plugin.server.scheduler.buildTask(plugin, task).schedule()
    }

    override fun runTaskLater(delay: Long, unit: TimeUnit, task: () -> Unit): Scheduler.Task {
        val scheduledTask = plugin.server.scheduler.buildTask(plugin, task)
            .delay(delay, unit)
            .schedule()

        return VelocityGradewayTask(scheduledTask)
    }

    override fun runTaskTimer(
        delay: Long,
        delayUnit: TimeUnit,
        interval: Long,
        intervalUnit: TimeUnit,
        task: () -> Unit
    ): Scheduler.Task {
        val scheduledTask = plugin.server.scheduler.buildTask(plugin, task)
            .delay(delay, delayUnit)
            .repeat(interval, intervalUnit)
            .schedule()

        return VelocityGradewayTask(scheduledTask)
    }

    class VelocityGradewayTask(val task: ScheduledTask) : Scheduler.Task {
        override fun cancel(): Boolean {
            val status = task.status()
            if (status == TaskStatus.CANCELLED || status == TaskStatus.FINISHED) {
                return false
            }
            task.cancel()
            return true
        }
    }
}
