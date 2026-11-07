/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.database.models.log

import dev.gradienttim.gradeway.constants.TableConstants
import dev.gradienttim.gradeway.entity.log.AuditLogEntity
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.dao.java.UUIDEntity
import org.jetbrains.exposed.v1.dao.java.UUIDEntityClass
import org.jetbrains.exposed.v1.javatime.timestamp
import java.util.*

object AuditLogsTable : UUIDTable(name = TableConstants.AUDIT_LOGS_TABLE_NAME) {
    val actorType = varchar("actor_type", 32)
    val actorIdentifier = varchar("actor_identifier", 255).nullable()

    val targetType = varchar("target_type", 32)
    val targetId = javaUUID("target_id")
    val targetSecondaryId = javaUUID("target_secondary_id").nullable()

    val timestamp = timestamp("timestamp")
    val action = varchar("action", 64)

    val before = text("before").nullable()
    val after = text("after").nullable()
}

class DatabaseAuditLogEntity(id: EntityID<UUID>) : UUIDEntity(id), AuditLogEntity {
    companion object : UUIDEntityClass<DatabaseAuditLogEntity>(AuditLogsTable)

    override var actorType by AuditLogsTable.actorType
    override var actorIdentifier by AuditLogsTable.actorIdentifier

    override var targetType by AuditLogsTable.targetType
    override var targetId by AuditLogsTable.targetId
    override var targetSecondaryId by AuditLogsTable.targetSecondaryId

    override var action by AuditLogsTable.action
    override var timestamp by AuditLogsTable.timestamp

    override var before by AuditLogsTable.before
    override var after by AuditLogsTable.after
}
