/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.database.models.permission

import dev.gradienttim.gradeway.audit.Actor
import dev.gradienttim.gradeway.audit.Target
import dev.gradienttim.gradeway.constants.TableConstants
import dev.gradienttim.gradeway.entity.permission.PermissionTemplateEntity
import dev.gradienttim.gradeway.services.PermissionService
import dev.gradienttim.gradeway.utilities.serialize.JsonSerializable
import kotlinx.serialization.json.*
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.dao.java.UUIDEntity
import org.jetbrains.exposed.v1.dao.java.UUIDEntityClass
import org.jetbrains.exposed.v1.javatime.timestamp
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Instant
import java.util.*

object PermissionTemplatesTable : UUIDTable(name = TableConstants.PERMISSION_TEMPLATES_TABLE_NAME) {
    val name = varchar("name", TableConstants.PERMISSION_TEMPLATES_TABLE_MAX_NAME_LENGTH).uniqueIndex()
    val assignedTo =
        enumeration<PermissionTemplateEntity.AssignedTo>("assigned_to").default(PermissionTemplateEntity.AssignedTo.ALL)

    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
    val updatedAt = timestamp("updated_at").clientDefault { Instant.now() }
}

class DatabasePermissionTemplateEntity(id: EntityID<UUID>) : UUIDEntity(id), PermissionTemplateEntity, KoinComponent {
    companion object : UUIDEntityClass<DatabasePermissionTemplateEntity>(PermissionTemplatesTable),
        JsonSerializable<DatabasePermissionTemplateEntity> {
        override fun serialize(data: DatabasePermissionTemplateEntity): JsonObject = buildJsonObject {
            put("id", data.id.value.toString())
            put("name", data.name)
            put("assignedTo", data.assignedTo.name)
            put("createdAt", data.createdAt.toEpochMilli())
            put("updatedAt", data.updatedAt.toEpochMilli())
        }

        override fun deserialize(json: JsonObject): DatabasePermissionTemplateEntity {
            val id = UUID.fromString(json.getValue("id").jsonPrimitive.content)

            return new(id) {
                name = json.getValue("name").jsonPrimitive.content
                assignedTo = PermissionTemplateEntity.AssignedTo
                    .valueOf(json.getValue("assignedTo").jsonPrimitive.content)

                createdAt = Instant.ofEpochMilli(json.getValue("createdAt").jsonPrimitive.long)
                updatedAt = Instant.ofEpochMilli(json.getValue("updatedAt").jsonPrimitive.long)
            }
        }
    }

    override val target by lazy { Target.permissionTemplate(id.value) }

    internal val permissionService: PermissionService by inject()

    override var name by PermissionTemplatesTable.name
    override var assignedTo by PermissionTemplatesTable.assignedTo

    override var createdAt by PermissionTemplatesTable.createdAt
    override var updatedAt by PermissionTemplatesTable.updatedAt

    override val permissions by DatabasePermissionTemplatePermissionEntity referrersOn
            PermissionTemplatePermissionsTable.templateId

    override fun setName(actor: Actor, name: String) =
        permissionService.setTemplateName(actor, this, name)

    override fun setAssignedTo(actor: Actor, assignedTo: PermissionTemplateEntity.AssignedTo) =
        permissionService.setTemplateAssignedTo(actor, this, assignedTo)

    override fun snapshot(): JsonObject = buildJsonObject {
        put("id", id.value.toString())
        put("name", name)
        put("assignedTo", assignedTo.name)
        put("createdAt", createdAt.toEpochMilli())
        put("updatedAt", updatedAt.toEpochMilli())
    }
}
