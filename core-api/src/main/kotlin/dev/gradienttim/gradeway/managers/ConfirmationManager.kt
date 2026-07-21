/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.managers

import arrow.core.Either
import dev.gradienttim.gradeway.utilities.lifecycle.Disableable
import dev.gradienttim.gradeway.utilities.lifecycle.Loadable
import dev.gradienttim.gradeway.utilities.lifecycle.Unloadable
import net.kyori.adventure.audience.Audience
import java.util.concurrent.ScheduledFuture

/**
 * Interface for managing confirmation operations, including scheduling tasks for confirmation
 * and handling task cancellations. Implementors of this interface are expected to manage the
 * lifecycle of scheduled confirmations and ensure proper cleanup when disabled.
 */
interface ConfirmationManager : Loadable, Unloadable, Disableable {
    val jobs: Set<Job>

    /**
     * Schedules a task for execution and sets up a timeout handler for the task.
     *
     * @param sender The sender who requested the task.
     * @param task The function representing the task to be scheduled for execution.
     * @param onTimeout The function to invoke when the task times out. Receives the task's unique identifier as a parameter.
     * @return An Either containing a [RequestJobError] if the task scheduling fails,
     *         or a String representing the unique identifier of the scheduled task if successful.
     */
    fun request(sender: Audience, task: () -> Unit, onTimeout: (id: String) -> Unit): Either<RequestJobError, String>

    /**
     * Confirms a scheduled task identified by its unique identifier.
     *
     * @param sender The sender who wants to confirm the task.
     * @param id The unique identifier of the scheduled confirmation task to confirm.
     * @return An [Either] containing a [ConfirmJobError] if the confirmation fails,
     *         or [Unit] if the confirmation is successful.
     */
    fun confirm(sender: Audience, id: String): Either<ConfirmJobError, Unit>

    /**
     * Cancels a scheduled confirmation task identified by its unique identifier.
     *
     * @param sender The sender who wants to cancel the task.
     * @param id The unique identifier of the task to cancel.
     * @return An [Either] containing a [CancelJobError] if the cancellation fails,
     *         or [Unit] if the task is successfully canceled.
     */
    fun cancel(sender: Audience, id: String): Either<CancelJobError, Unit>

    /**
     * Finds a scheduled confirmation task by its unique identifier.
     *
     * @param id The unique identifier of the confirmation task to search for.
     * @return The corresponding Job if a task with the given ID exists, or null if no such task is found.
     */
    fun find(id: String): Job? = jobs.find { it.id == id }

    data class Job(
        val id: String,
        val task: () -> Unit,
        val scheduler: ScheduledFuture<*>,
        val sender: Audience
    ) {
        fun cancel(): Boolean {
            if (!scheduler.isCancelled) {
                return scheduler.cancel(true)
            }
            return false
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Job) return false
            return id == other.id && sender == other.sender
        }

        override fun hashCode(): Int = 31 * id.hashCode() + sender.hashCode()
    }

    sealed interface RequestJobError {
        object FailedToRegister : RequestJobError
        data class Unexpected(val throwable: Throwable) : RequestJobError
    }

    sealed interface ConfirmJobError {
        object NotRegistered : ConfirmJobError
        object WrongSender : ConfirmJobError
        data class Unexpected(val throwable: Throwable) : ConfirmJobError
    }

    sealed interface CancelJobError {
        object NotRegistered : CancelJobError
        object WrongSender : CancelJobError
        data class Unexpected(val throwable: Throwable) : CancelJobError
    }
}
