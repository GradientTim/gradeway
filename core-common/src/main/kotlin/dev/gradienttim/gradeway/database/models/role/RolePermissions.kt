/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.database.models.role

import dev.gradienttim.gradeway.constants.TableConstants
import dev.gradienttim.gradeway.database.models.permission.DatabasePermissionEntity
import dev.gradienttim.gradeway.database.models.permission.PermissionsTable
import dev.gradienttim.gradeway.entity.role.RolePermissionEntity
import dev.gradienttim.gradeway.utilities.Serializable
import kotlinx.serialization.json.*
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.CompositeID
import org.jetbrains.exposed.v1.core.dao.id.CompositeIdTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.CompositeEntity
import org.jetbrains.exposed.v1.dao.CompositeEntityClass
import java.util.*

object RolePermissionsTable : CompositeIdTable(name = TableConstants.ROLE_PERMISSIONS_TABLE_NAME) {
    val roleId = reference(
        name = "role_id",
        refColumn = RolesTable.id,
        onUpdate = ReferenceOption.CASCADE,
        onDelete = ReferenceOption.CASCADE
    )

    val permissionId = reference(
        name = "permission_id",
        refColumn = PermissionsTable.id,
        onUpdate = ReferenceOption.CASCADE,
        onDelete = ReferenceOption.CASCADE
    )

    val isEnabled = bool("is_enabled").default(true)

    init {
        addIdColumn(roleId)
        addIdColumn(permissionId)
    }
}

class DatabaseRolePermissionEntity(id: EntityID<CompositeID>) : CompositeEntity(id), RolePermissionEntity {
    companion object : CompositeEntityClass<DatabaseRolePermissionEntity>(RolePermissionsTable),
        Serializable<DatabaseRolePermissionEntity> {
        override fun serialize(instance: DatabaseRolePermissionEntity): JsonObject = buildJsonObject {
            put("roleId", instance.roleId.value.toString())
            put("permissionId", instance.permissionId.value.toString())
            put("isEnabled", instance.isEnabled)
        }

        override fun deserialize(json: JsonObject): DatabaseRolePermissionEntity = new {
            roleId = EntityID(
                id = UUID.fromString(json.getValue("roleId").jsonPrimitive.content),
                table = RolesTable
            )

            permissionId = EntityID(
                id = UUID.fromString(json.getValue("permissionId").jsonPrimitive.content),
                table = PermissionsTable
            )

            isEnabled = json.getValue("isEnabled").jsonPrimitive.boolean
        }
    }

    override var roleId by RolePermissionsTable.roleId
    override var permissionId by RolePermissionsTable.permissionId

    override var isEnabled by RolePermissionsTable.isEnabled

    override val role by DatabaseRoleEntity referencedOn RolePermissionsTable.roleId
    override val permission by DatabasePermissionEntity referencedOn RolePermissionsTable.permissionId
}
