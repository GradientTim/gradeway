/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.database.models.permission

import dev.gradienttim.gradeway.constants.TableConstants
import dev.gradienttim.gradeway.entity.permission.PermissionTemplatePermissionEntity
import dev.gradienttim.gradeway.utilities.serialize.JsonSerializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.CompositeID
import org.jetbrains.exposed.v1.core.dao.id.CompositeIdTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.CompositeEntity
import org.jetbrains.exposed.v1.dao.CompositeEntityClass
import java.util.*

object PermissionTemplatePermissionsTable :
    CompositeIdTable(name = TableConstants.PERMISSION_TEMPLATE_PERMISSIONS_TABLE_NAME) {
    val templateId = reference(
        name = "template_id",
        refColumn = PermissionTemplatesTable.id,
        onUpdate = ReferenceOption.CASCADE,
        onDelete = ReferenceOption.CASCADE
    )

    val permissionId = reference(
        name = "permission_id",
        refColumn = PermissionsTable.id,
        onUpdate = ReferenceOption.CASCADE,
        onDelete = ReferenceOption.CASCADE
    )

    init {
        addIdColumn(templateId)
        addIdColumn(permissionId)
    }
}

class DatabasePermissionTemplatePermissionEntity(id: EntityID<CompositeID>) : CompositeEntity(id),
    PermissionTemplatePermissionEntity {
    companion object :
        CompositeEntityClass<DatabasePermissionTemplatePermissionEntity>(PermissionTemplatePermissionsTable),
        JsonSerializable<DatabasePermissionTemplatePermissionEntity> {
        override fun serialize(data: DatabasePermissionTemplatePermissionEntity): JsonObject = buildJsonObject {
            put("templateId", data.templateId.value.toString())
            put("permissionId", data.permissionId.value.toString())
        }

        override fun deserialize(json: JsonObject): DatabasePermissionTemplatePermissionEntity = new {
            templateId = EntityID(
                id = UUID.fromString(json.getValue("templateId").jsonPrimitive.content),
                table = PermissionTemplatesTable
            )

            permissionId = EntityID(
                id = UUID.fromString(json.getValue("permissionId").jsonPrimitive.content),
                table = PermissionsTable
            )
        }
    }

    override var templateId by PermissionTemplatePermissionsTable.templateId
    override var permissionId by PermissionTemplatePermissionsTable.permissionId

    override val template by DatabasePermissionTemplateEntity referencedOn PermissionTemplatePermissionsTable.templateId
    override val permission by DatabasePermissionEntity referencedOn PermissionTemplatePermissionsTable.permissionId
}
