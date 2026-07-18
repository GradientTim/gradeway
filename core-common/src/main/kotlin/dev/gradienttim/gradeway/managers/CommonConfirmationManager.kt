/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.managers

import arrow.core.Either
import arrow.core.raise.either
import dev.gradienttim.gradeway.managers.CommonConfirmationManager.Companion.JOB_ID_ALPHABET
import dev.gradienttim.gradeway.managers.CommonConfirmationManager.Companion.JOB_ID_LENGTH
import dev.gradienttim.gradeway.managers.ConfirmationManager.*
import net.kyori.adventure.audience.Audience
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class CommonConfirmationManager : ConfirmationManager {
    override val jobs = mutableSetOf<Job>()

    private lateinit var executorService: ScheduledExecutorService

    override fun load(): Either<Throwable, Unit> = either {
        try {
            if (!::executorService.isInitialized || executorService.isShutdown) {
                executorService = Executors.newSingleThreadScheduledExecutor()
            }
        } catch (throwable: Throwable) {
            raise(throwable)
        }
    }

    override fun unload(): Either<Throwable, Unit> = either {
        try {
            if (::executorService.isInitialized) {
                executorService.shutdown()
            }
        } catch (throwable: Throwable) {
            raise(throwable)
        }
    }

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
        task: () -> Unit,
        onTimeout: (id: String) -> Unit
    ): Either<RequestJobError, String> = either {
        try {
            val id = generateJobId()

            val scheduler = executorService.schedule({
                onTimeout(id)
                cancel(sender, id)
            }, 1L, TimeUnit.MINUTES)

            if (!jobs.add(Job(id, task, scheduler, sender))) {
                scheduler.cancel(true)
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
            job.task()
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
