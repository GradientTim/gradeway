/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.database.models.group

import dev.gradienttim.gradeway.constants.TableConstants
import dev.gradienttim.gradeway.database.models.permission.DatabasePermissionTemplateEntity
import dev.gradienttim.gradeway.database.models.permission.PermissionTemplatesTable
import dev.gradienttim.gradeway.entity.group.GroupPermissionTemplateEntity
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

object GroupPermissionTemplatesTable : CompositeIdTable(name = TableConstants.GROUP_PERMISSION_TEMPLATES_TABLE_NAME) {
    val groupId = reference(
        name = "group_id",
        refColumn = GroupsTable.id,
        onUpdate = ReferenceOption.CASCADE,
        onDelete = ReferenceOption.CASCADE
    )

    val permissionTemplateId = reference(
        name = "permission_template_id",
        refColumn = PermissionTemplatesTable.id,
        onUpdate = ReferenceOption.CASCADE,
        onDelete = ReferenceOption.CASCADE
    )
}

class DatabaseGroupPermissionTemplateEntity(id: EntityID<CompositeID>) : CompositeEntity(id),
    GroupPermissionTemplateEntity {
    companion object : CompositeEntityClass<DatabaseGroupPermissionTemplateEntity>(GroupPermissionTemplatesTable),
        JsonSerializable<DatabaseGroupPermissionTemplateEntity> {
        override fun serialize(data: DatabaseGroupPermissionTemplateEntity): JsonObject = buildJsonObject {
            put("groupId", data.groupId.value.toString())
            put("permissionTemplateId", data.permissionTemplateId.value.toString())
        }

        override fun deserialize(json: JsonObject): DatabaseGroupPermissionTemplateEntity = new {
            groupId = EntityID(
                id = UUID.fromString(json.getValue("groupId").jsonPrimitive.content),
                table = GroupsTable
            )

            permissionTemplateId = EntityID(
                id = UUID.fromString(json.getValue("permissionTemplateId").jsonPrimitive.content),
                table = PermissionTemplatesTable
            )
        }
    }

    override var groupId by GroupPermissionTemplatesTable.groupId
    override var permissionTemplateId by GroupPermissionTemplatesTable.permissionTemplateId

    override val group by DatabaseGroupEntity referencedOn GroupPermissionTemplatesTable.groupId
    override val permissionTemplate by DatabasePermissionTemplateEntity referencedOn
            GroupPermissionTemplatesTable.permissionTemplateId
}
