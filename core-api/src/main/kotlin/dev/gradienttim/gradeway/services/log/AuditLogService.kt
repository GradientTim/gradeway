/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.services.log

import arrow.core.Either
import dev.gradienttim.gradeway.audit.Actor
import dev.gradienttim.gradeway.audit.Snapshot
import dev.gradienttim.gradeway.entity.log.AuditLogEntity
import org.jetbrains.exposed.v1.jdbc.SizedIterable
import java.util.*

interface AuditLogService {
    /**
     * Creates an audit log entry with the details of an operation.
     *
     * @param actor The actor performing the operation.
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

    /**
     * Retrieves an audit log entry by its unique identifier.
     *
     * @param id The unique identifier of the audit log entry.
     * @return The `AuditLogEntity` corresponding to the provided ID, or null if no entry is found.
     */
    fun findById(id: UUID): AuditLogEntity?

    /**
     * Retrieves all audit log entries recorded for the given actor, newest first.
     *
     * Entries are matched on both the actor's type and its identifier, so actors without an
     * identifier (e.g. [Actor.System], [Actor.Console]) match every entry of their type.
     *
     * @param actor The actor whose audit log entries should be retrieved.
     * @return A `SizedIterable` containing the matching `AuditLogEntity` instances, ordered by timestamp descending.
     */
    fun findByActor(actor: Actor): SizedIterable<AuditLogEntity>

    sealed interface CreateAuditLogError {
        data class Unexpected(val throwable: Throwable) : CreateAuditLogError
    }

    sealed interface DeleteAuditLogError {
        data object EntityNotFound : DeleteAuditLogError
        data class Unexpected(val throwable: Throwable) : DeleteAuditLogError
    }
}
