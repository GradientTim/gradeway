/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.database.models.role

import dev.gradienttim.gradeway.constants.TableConstants
import dev.gradienttim.gradeway.database.models.permission.DatabasePermissionTemplateEntity
import dev.gradienttim.gradeway.database.models.permission.PermissionTemplatesTable
import dev.gradienttim.gradeway.entity.role.RolePermissionTemplateEntity
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

object RolePermissionTemplatesTable : CompositeIdTable(name = TableConstants.ROLE_PERMISSION_TEMPLATES_TABLE_NAME) {
    val roleId = reference(
        name = "role_id",
        refColumn = RolesTable.id,
        onUpdate = ReferenceOption.CASCADE,
        onDelete = ReferenceOption.CASCADE
    )

    val permissionTemplateId = reference(
        name = "permission_template_id",
        refColumn = PermissionTemplatesTable.id,
        onUpdate = ReferenceOption.CASCADE,
        onDelete = ReferenceOption.CASCADE
    )

    init {
        addIdColumn(roleId)
        addIdColumn(permissionTemplateId)
        uniqueIndex(roleId, permissionTemplateId)
    }
}

class DatabaseRolePermissionTemplateEntity(id: EntityID<CompositeID>) : CompositeEntity(id),
    RolePermissionTemplateEntity {
    companion object : CompositeEntityClass<DatabaseRolePermissionTemplateEntity>(RolePermissionTemplatesTable),
        JsonSerializable<DatabaseRolePermissionTemplateEntity> {
        override fun serialize(data: DatabaseRolePermissionTemplateEntity) = buildJsonObject {
            put("roleId", data.roleId.value.toString())
            put("permissionTemplateId", data.permissionTemplateId.value.toString())
        }

        override fun deserialize(json: JsonObject) = new {
            roleId = EntityID(
                id = UUID.fromString(json.getValue("roleId").jsonPrimitive.content),
                table = RolesTable
            )

            permissionTemplateId = EntityID(
                id = UUID.fromString(json.getValue("permissionTemplateId").jsonPrimitive.content),
                table = PermissionTemplatesTable
            )
        }
    }

    override var roleId by RolePermissionTemplatesTable.roleId
    override var permissionTemplateId by RolePermissionTemplatesTable.permissionTemplateId

    override val role by DatabaseRoleEntity referencedOn RolePermissionTemplatesTable.roleId
    override val permissionTemplate by DatabasePermissionTemplateEntity referencedOn
            RolePermissionTemplatesTable.permissionTemplateId
}
