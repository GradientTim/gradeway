package dev.gradienttim.gradeway.services

import arrow.core.Either
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.audit.Actor
import dev.gradienttim.gradeway.audit.Snapshot
import dev.gradienttim.gradeway.entity.log.AuditLogEntity
import dev.gradienttim.gradeway.services.log.AuditLogService
import java.util.UUID

class CommonLogService(
    val gradeway: CommonGradeway<*>,
) : LogService {
    override fun createAuditLog(
        actor: Actor,
        action: String,
        snapshot: Snapshot
    ): Either<AuditLogService.CreateAuditLogError, AuditLogEntity> {
        TODO("Not yet implemented")
    }

    override fun deleteAuditLog(id: UUID): Either<AuditLogService.DeleteAuditLogError, Unit> {
        TODO("Not yet implemented")
    }

    override fun deleteAuditLog(entity: AuditLogEntity): Either<AuditLogService.DeleteAuditLogError, Unit> {
        TODO("Not yet implemented")
    }
}
