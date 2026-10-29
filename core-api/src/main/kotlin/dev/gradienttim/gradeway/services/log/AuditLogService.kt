/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.services.log

import arrow.core.Either
import dev.gradienttim.gradeway.audit.Actor
import dev.gradienttim.gradeway.audit.Snapshot
import dev.gradienttim.gradeway.entity.log.AuditLogEntity
import java.util.*

interface AuditLogService {
    /**
     * Creates an audit log entry with the details of an operation.
     *
     * @param actor The actor responsible for triggering the action. Must be an instance of `Actor`.
     * @param action A string representing the action performed (e.g., "update", "delete").
     * @param snapshot The target entity involved in the operation and its state before and after it.
     * @return Either a `CreateAuditLogError` if the operation fails or an `AuditLogEntity` representing the created audit log entry.
     */
    fun createAuditLog(
        actor: Actor,
        action: String,
        snapshot: Snapshot,
    ): Either<CreateAuditLogError, AuditLogEntity>

    /**
     * Deletes an audit log entry identified by its unique identifier.
     *
     * @param id The unique identifier of the audit log entry to be deleted.
     * @return Either an instance of [DeleteAuditLogError] if the deletion fails,
     *         or [Unit] if the deletion is successful.
     */
    fun deleteAuditLog(id: UUID): Either<DeleteAuditLogError, Unit>

    /**
     * Deletes the specified audit log entry.
     *
     * @param entity The audit log entry to be deleted. Must be an instance of `AuditLogEntity`, representing
     *               the log record to remove from the system.
     * @return Either an instance of `DeleteAuditLogError` if the deletion fails, or `Unit` if the operation is successful.
     */
    fun deleteAuditLog(entity: AuditLogEntity): Either<DeleteAuditLogError, Unit>

    sealed interface CreateAuditLogError {
        data class Unexpected(val throwable: Throwable) : CreateAuditLogError
    }

    sealed interface DeleteAuditLogError {
        data class Unexpected(val throwable: Throwable) : DeleteAuditLogError
    }
}
