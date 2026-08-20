/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.platform

import java.util.concurrent.TimeUnit

/**
 * Represents a scheduler interface for managing and executing tasks in a platform-independent manner.
 * Provides methods for running tasks immediately, with a delay, or on a repeating timer.
 */
interface Scheduler {
    /**
     * Executes the provided task immediately on the current thread.
     *
     * @param task A lambda function representing the task to be executed.
     */
    fun runTask(task: () -> Unit)

    /**
     * Schedules a task to be executed after a specified delay.
     *
     * @param delay The amount of time to wait before executing the task. Defaults to `1`.
     * @param unit The time unit of the delay parameter. Defaults to `TimeUnit.MILLISECONDS`.
     * @param task The lambda function representing the task to be executed.
     * @return An instance of [Task] that can be used to cancel the scheduled task.
     */
    fun runTaskLater(delay: Long = 1, unit: TimeUnit = TimeUnit.MILLISECONDS, task: () -> Unit): Task

    /**
     * Schedules a task to run on a repeating timer with an optional initial delay.
     *
     * @param delay The initial delay before the task is first executed. Defaults to `1`.
     * @param delayUnit The time unit for the delay parameter. Defaults to `TimeUnit.MILLISECONDS`.
     * @param interval The interval between consecutive executions of the task. Defaults to `1`.
     * @param intervalUnit The time unit for the period parameter. Defaults to `TimeUnit.SECONDS`.
     * @param task The lambda function representing the task to be executed repeatedly.
     * @return An instance of [Task] that can be used to cancel the scheduled task.
     */
    fun runTaskTimer(
        delay: Long = 1,
        delayUnit: TimeUnit = TimeUnit.MILLISECONDS,
        interval: Long = 1,
        intervalUnit: TimeUnit = TimeUnit.SECONDS,
        task: () -> Unit
    ): Task

    /**
     * Represents a cancellable task, typically used with a scheduler to manage
     * execution of operations in a controlled and interruptible manner.
     *
     * Tasks can be canceled if they are no longer required to prevent unnecessary
     * execution or resource consumption.
     */
    interface Task {
        /**
         * Cancels the execution of a task.
         * This method is intended to prevent the task from continuing or starting if it is already scheduled or running.
         * Once a task is canceled, its associated resources may be released, and it will no longer be executed.
         *
         * @return `true` if the task was successfully canceled, `false` if the task was already canceled or could not be canceled.
         */
        fun cancel(): Boolean
    }
}
