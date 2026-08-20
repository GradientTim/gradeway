/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.managers

import arrow.core.Either
import arrow.core.raise.either
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.managers.ConfirmationManager.*
import net.kyori.adventure.audience.Audience
import java.util.concurrent.TimeUnit

class CommonConfirmationManager(val gradeway: CommonGradeway<*>) : ConfirmationManager {
    override val jobs = mutableSetOf<Job>()

    override fun disable(): Either<Throwable, Unit> = either {
        try {
            jobs.forEach { it.cancel() }
            jobs.clear()
        } catch (throwable: Throwable) {
            raise(throwable)
        }
    }

    override fun request(
        sender: Audience,
        handler: () -> Unit,
        onTimeout: (id: String) -> Unit
    ): Either<RequestJobError, String> = either {
        try {
            val id = generateJobId()

            val task = gradeway.scheduler.runTaskLater(
                delay = 1L,
                unit = TimeUnit.MINUTES
            ) {
                onTimeout(id)
                cancel(sender, id)
            }

            if (!jobs.add(Job(id, task, handler, sender))) {
                task.cancel()
                raise(RequestJobError.FailedToRegister)
            }

            id
        } catch (throwable: Throwable) {
            raise(RequestJobError.Unexpected(throwable))
        }
    }

    override fun confirm(sender: Audience, id: String): Either<ConfirmJobError, Unit> = either {
        val job = find(id) ?: raise(ConfirmJobError.NotRegistered)

        if (job.sender != sender) {
            raise(ConfirmJobError.WrongSender)
        }

        try {
            job.cancel()
            job.handler()
            jobs.remove(job)
        } catch (throwable: Throwable) {
            raise(ConfirmJobError.Unexpected(throwable))
        }
    }

    override fun cancel(sender: Audience, id: String): Either<CancelJobError, Unit> = either {
        val job = find(id) ?: raise(CancelJobError.NotRegistered)

        if (job.sender != sender) {
            raise(CancelJobError.WrongSender)
        }

        try {
            job.cancel()
            jobs.remove(job)
        } catch (throwable: Throwable) {
            raise(CancelJobError.Unexpected(throwable))
        }
    }

    private fun generateJobId(): String {
        var id: String
        do {
            id = (1..JOB_ID_LENGTH)
                .map { JOB_ID_ALPHABET.random() }
                .joinToString("")
        } while (find(id) != null)

        return id
    }

    companion object {
        /**
         * The characters eligible for use in a generated job identifier.
         *
         * Ambiguous characters (`0`/`O`, `1`/`I`/`l`) are excluded so that identifiers
         * remain easy to read and re-type when a user confirms or cancels a job.
         */
        private const val JOB_ID_ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
        private const val JOB_ID_LENGTH = 6
    }
}
