/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.services

import arrow.core.Either
import arrow.core.raise.either
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.audit.Actor
import dev.gradienttim.gradeway.audit.AuditNotification
import dev.gradienttim.gradeway.audit.Snapshot
import dev.gradienttim.gradeway.database.models.log.AuditLogsTable
import dev.gradienttim.gradeway.database.models.log.DatabaseAuditLogEntity
import dev.gradienttim.gradeway.entity.log.AuditLogEntity
import dev.gradienttim.gradeway.services.log.AuditLogService
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.SizedIterable
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.Instant
import java.util.UUID

class CommonLogService(
    val gradeway: CommonGradeway<*>,
) : LogService {
    override fun createAuditLog(
        actor: Actor,
        action: String,
        snapshot: Snapshot,
    ): Either<AuditLogService.CreateAuditLogError, DatabaseAuditLogEntity> = either {
        try {
            transaction(gradeway.database) {
                DatabaseAuditLogEntity.new {
                    this.actorType = actor.type
                    this.actorIdentifier = actor.identifier()
                    this.targetType = snapshot.target.type
                    this.targetId = snapshot.target.id
                    this.targetSecondaryId = snapshot.target.secondaryId
                    this.timestamp = Instant.now()
                    this.action = action
                    this.before = snapshot.before?.toString()
                    this.after = snapshot.after?.toString()
                }
            }
        } catch (throwable: Throwable) {
            raise(AuditLogService.CreateAuditLogError.Unexpected(throwable))
        }
    }.onRight { entry ->
        if (gradeway.configs.gradewayEntry.config.auditLog.notify) {
            gradeway.notifications.notify(
                AuditNotification(
                    actor = actor,
                    action = action,
                    target = snapshot.target,
                    before = snapshot.before,
                    after = snapshot.after,
                    timestamp = entry.timestamp,
                ),
            )
        }
    }

    override fun deleteAuditLog(id: UUID): Either<AuditLogService.DeleteAuditLogError, Unit> = either {
        val entity = findById(id) ?: raise(AuditLogService.DeleteAuditLogError.EntityNotFound)
        return deleteAuditLog(entity)
    }

    override fun deleteAuditLog(entity: AuditLogEntity): Either<AuditLogService.DeleteAuditLogError, Unit> = either {
        if (entity !is DatabaseAuditLogEntity) {
            val throwable = Throwable("Entity is not a type of DatabaseAuditLogEntity")
            raise(AuditLogService.DeleteAuditLogError.Unexpected(throwable))
        }
        try {
            transaction(gradeway.database) {
                entity.delete()
            }
        } catch (throwable: Throwable) {
            raise(AuditLogService.DeleteAuditLogError.Unexpected(throwable))
        }
    }

    override fun findById(id: UUID): DatabaseAuditLogEntity? {
        return transaction(gradeway.database) {
            DatabaseAuditLogEntity.findById(id)
        }
    }

    override fun findByActor(actor: Actor): SizedIterable<DatabaseAuditLogEntity> {
        val identifier = actor.identifier()
        return transaction(gradeway.database) {
            DatabaseAuditLogEntity.find {
                val byType = AuditLogsTable.actorType eq actor.type
                if (identifier == null) {
                    byType
                } else {
                    byType and (AuditLogsTable.actorIdentifier eq identifier)
                }
            }.orderBy(AuditLogsTable.timestamp to SortOrder.DESC)
        }
    }
}
